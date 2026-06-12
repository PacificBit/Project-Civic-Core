package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.miner;

import java.util.UUID;
import java.util.function.Supplier;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineshaftGallery;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.miner.BuilderPlaceMineshaftGalleryPrefabAction;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.MinerScan;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class PlaceMineshaftGalleryPrefabAction extends VillagerActionBase {
  private final PrefabType prefabType;
  private final String snapshotAnchorGroup;
  private final String commitLastDiggedFromSnapshotGroup;

  public PlaceMineshaftGalleryPrefabAction(
      BuilderPlaceMineshaftGalleryPrefabAction builderActionBase,
      BuilderSupport support) {
    super(builderActionBase);
    this.prefabType = builderActionBase.getPrefabType(support);
    this.snapshotAnchorGroup = builderActionBase.getSnapshotAnchorGroup(support);
    this.commitLastDiggedFromSnapshotGroup = builderActionBase.getCommitLastDiggedFromSnapshotGroup(support);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      ExecutionSupport executionSupport,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.canExecute(ref, executionSupport, sensorInfo, deltaTime, store)) {
      return false;
    }
    super.execute(ref, executionSupport, sensorInfo, deltaTime, store);

    WorkplaceAreaSnapshot snapshot = this.getAreaSnapshot(ref, store);
    UUID villagerId = this.getVillagerId(ref, store);
    MineComponent mineComponent = this.getComponentFromWorkplace(store, ref, MineComponent.getComponentType());
    WorkplaceComponent workplace = this.getWorkplaceComponent(ref, store);
    if (snapshot == null || villagerId == null || mineComponent == null || workplace == null) {
      return false;
    }

    MineshaftGallery mineshaftGallery = mineComponent.getWorkerAllocatedGallery(villagerId);
    if (mineshaftGallery == null) {
      Logger.logWarning("Could not fetch mineshaft gallery allocated to the villager: "
          + villagerId);
      return false;
    }

    Vec3i galleryDirection = mineshaftGallery.getCardinalDirection();
    if (galleryDirection == null) {
      Logger.logWarning("Gallery is missing direction");
      return false;
    }

    int depth = StructureConfig.Mine.MineshaftGalleryConf.SIZE_2I.y;
    Vec3i galleryTarget = this.resolveGalleryTarget(snapshot, mineshaftGallery, galleryDirection, depth);
    if (galleryTarget == null) {
      Logger.logWarning("Could not resolve gallery target");
      return false;
    }

    Vec3i committedDigPosition = this.resolveCommittedDigPosition(snapshot, mineshaftGallery, galleryDirection,
        depth);
    if (this.hasValue(this.commitLastDiggedFromSnapshotGroup) && committedDigPosition == null) {
      Logger.logWarning(
          "Could not resolve commit position from snapshot group "
              + this.commitLastDiggedFromSnapshotGroup);
      return false;
    }

    Rotation galleryRotation = Utils.getRotationFromCardinalDirection(galleryDirection);
    World world = this.getWorld(store);
    boolean shouldCaptureRestoreSnapshot = this.prefabType.equals(PrefabType.EMPTY)
        && !mineshaftGallery.hasRestoreSnapshotAt(galleryTarget);

    TrackedPrefabPlacementComponent restoreSnapshot = this.prefabType.equals(PrefabType.EMPTY)
        ? shouldCaptureRestoreSnapshot
            ? PrefabManager.captureTrackedPrefabSnapshot(
                world,
                StructureConfig.Mine.MineshaftGalleryConf.PREFAB_JSON,
                galleryTarget,
                galleryRotation)
            : null
        : PrefabManager.spawnTrackedPrefab(
            world,
            this.prefabType.getPrefab(),
            galleryTarget,
            galleryRotation,
            true);

    if (!this.prefabType.equals(PrefabType.EMPTY) && restoreSnapshot == null) {
      Logger.logWarning("Could not spawn prefab");
      return false;
    }
    if (shouldCaptureRestoreSnapshot && restoreSnapshot == null) {
      Logger.logWarning("Could not capture restore snapshot for section " + galleryTarget);
      return false;
    }

    boolean changedPersistentState = false;
    if (this.prefabType.equals(PrefabType.EMPTY)) {
      boolean restoreSnapshotAdded = restoreSnapshot != null && mineshaftGallery.addRestoreSnapshot(restoreSnapshot);
      changedPersistentState = restoreSnapshotAdded;
    }

    if (committedDigPosition != null) {
      mineshaftGallery.setLastDiggedPosition(Vec3i.from(committedDigPosition));
      if (this.prefabType.equals(PrefabType.BEAM)) {
        mineshaftGallery.checkIfGalleryIsCompleted();
      }
      this.clearSnapshotGroup(snapshot, this.snapshotAnchorGroup);
      this.clearSnapshotGroup(snapshot, this.commitLastDiggedFromSnapshotGroup);
      changedPersistentState = true;
    }

    if (changedPersistentState) {
      Utils.markNeedSaving(world, workplace.getBenchPos());
    }

    return true;
  }

  private Vec3i resolveGalleryTarget(
      WorkplaceAreaSnapshot snapshot,
      MineshaftGallery mineshaftGallery,
      Vec3i galleryDirection,
      int depth) {
    Vec3i snapshotTarget = this.resolveSnapshotPosition(snapshot, this.snapshotAnchorGroup);
    if (snapshotTarget != null) {
      return snapshotTarget;
    }

    Vec3i referenceDigPosition = this.resolveReferenceDigPosition(snapshot, mineshaftGallery);
    return this.resolveAlignedSectionAnchor(mineshaftGallery, referenceDigPosition, galleryDirection, depth);
  }

  private Vec3i resolveCommittedDigPosition(
      WorkplaceAreaSnapshot snapshot,
      MineshaftGallery mineshaftGallery,
      Vec3i galleryDirection,
      int depth) {
    if (!this.hasValue(this.commitLastDiggedFromSnapshotGroup)) {
      return null;
    }

    return this.resolveSnapshotPosition(snapshot, this.commitLastDiggedFromSnapshotGroup);
  }

  private Vec3i resolveReferenceDigPosition(
      WorkplaceAreaSnapshot snapshot,
      MineshaftGallery mineshaftGallery) {
    if (snapshot != null) {
      Vec3i nextDigPosition = snapshot.peekNextSnapshotTargetBlockFrom(MinerScan.DIG_POSITIONS);
      if (nextDigPosition != null) {
        return Vec3i.from(nextDigPosition);
      }
    }

    Vec3i lastDiggedPosition = mineshaftGallery == null ? null : mineshaftGallery.getLastDiggedPosition();
    return lastDiggedPosition == null ? null : Vec3i.from(lastDiggedPosition);
  }

  private Vec3i resolveAlignedSectionAnchor(
      MineshaftGallery mineshaftGallery,
      Vec3i referenceDigPosition,
      Vec3i galleryDirection,
      int depth) {
    Vec3i sectionStart = this.resolveCurrentSectionStart(mineshaftGallery, referenceDigPosition, galleryDirection,
        depth);
    return sectionStart == null ? null : Vec3i.from(sectionStart).add(Vec3i.from(galleryDirection).mul(depth));
  }

  private Vec3i resolveCurrentSectionStart(
      MineshaftGallery mineshaftGallery,
      Vec3i referenceDigPosition,
      Vec3i galleryDirection,
      int depth) {
    if (mineshaftGallery == null || referenceDigPosition == null || galleryDirection == null) {
      return null;
    }

    Vec3i galleryStartPosition = mineshaftGallery.getStartPosition();
    if (galleryStartPosition == null) {
      return null;
    }

    int travelledDistance = this.getTravelledDistanceAlongDirection(galleryStartPosition, referenceDigPosition,
        galleryDirection);
    if (travelledDistance <= 0) {
      return Vec3i.from(galleryStartPosition);
    }

    int sectionOffset = Math.floorDiv(travelledDistance, depth) * depth;
    return Vec3i.from(galleryStartPosition).add(Vec3i.from(galleryDirection).mul(sectionOffset));
  }

  private int getTravelledDistanceAlongDirection(
      Vec3i from,
      Vec3i to,
      Vec3i direction) {
    if (from == null || to == null || direction == null) {
      return 0;
    }

    if (direction.x != 0) {
      return (to.x - from.x) * Integer.signum(direction.x);
    }

    if (direction.z != 0) {
      return (to.z - from.z) * Integer.signum(direction.z);
    }

    return 0;
  }

  private Vec3i resolveSnapshotPosition(WorkplaceAreaSnapshot snapshot, String groupName) {
    if (!this.hasValue(groupName)) {
      return null;
    }

    Vec3i position = snapshot.peekNextSnapshotTargetBlockFrom(groupName);
    return position == null ? null : Vec3i.from(position);
  }

  private void clearSnapshotGroup(WorkplaceAreaSnapshot snapshot, String groupName) {
    if (!this.hasValue(groupName)) {
      return;
    }

    snapshot.removeSnapshotGroup(groupName);
  }

  private boolean hasValue(String value) {
    return value != null && !value.isEmpty();
  }

  public enum PrefabType implements Supplier<String> {
    EMPTY("Empty", StructureConfig.Mine.MineshaftGalleryConf.EMPTY_PREFAB_JSON),
    BEAM("Beam", StructureConfig.Mine.MineshaftGalleryConf.PREFAB_JSON);

    private final String type;
    private final String prefab;

    private PrefabType(String type, String prefab) {
      this.type = type;
      this.prefab = prefab;
    }

    public String getPrefab() {
      return this.prefab;
    }

    @Override
    public String get() {
      return this.type;
    }
  }
}
