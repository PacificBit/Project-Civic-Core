package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderReachWalkableSpaceNearSnapshotTargetSensor;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.BlockTarget;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class ReachWalkableSpaceNearSnapshotTargetSensor extends VillagerSensorBase {
  private final PositionProvider positionProvider = new PositionProvider();
  private final String[] blockIds;
  private final double stoppingRangeSquared;
  private final boolean useCenterOfBlock;
  private BlockTarget target = null;

  public ReachWalkableSpaceNearSnapshotTargetSensor(
      BuilderReachWalkableSpaceNearSnapshotTargetSensor builderSensorBase,
      BuilderSupport support) {
    super(builderSensorBase);
    this.blockIds = builderSensorBase.getBlockIds(support);

    double stoppingRange = builderSensorBase.getStoppingRange(support);
    this.stoppingRangeSquared = stoppingRange * stoppingRange;
    this.useCenterOfBlock = builderSensorBase.getUseCenterOfBlock(support);
  }

  @Override
  public InfoProvider getSensorInfo() {
    return this.positionProvider;
  }

  @Override
  public boolean matches(
      Ref<EntityStore> ref,
      ExecutionSupport executionSupport,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.matches(ref, executionSupport, deltaTime, store)) {
      this.positionProvider.clear();
      return false;
    }

    WorkplaceAreaSnapshot snapshot = this.getAreaSnapshot(ref, store);
    if (snapshot == null) {
      this.positionProvider.clear();
      return false;
    }

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      Logger.logWarning(
          "Could not find transform component");
      this.positionProvider.clear();
      return false;
    }

    this.findSnapshotTarget(snapshot);
    if (this.target == null) {
      this.positionProvider.clear();
      return false;
    }

    Vec3d targetPosition = getTargetPosition(store, transform);
    if (targetPosition == null) {
      this.positionProvider.clear();
      return false;
    }

    double distanceFromTarget = transform.getPosition().distanceSquared(targetPosition);
    if (distanceFromTarget <= this.stoppingRangeSquared) {
      this.positionProvider.clear();
      return false;
    }

    this.positionProvider.setTarget(targetPosition);
    return true;
  }

  private Vec3d getTargetPosition(Store<EntityStore> store, TransformComponent transform) {
    if (this.target.getAccessPosition() != null) {
      return Vec3d.from(this.target.getAccessPosition());
    }

    Vec3i blockTarget = this.target.getTargetBlock();
    if (blockTarget == null) {
      return null;
    }

    SnapshotTargetStandabilityInspector.Inspection standabilityInspection = SnapshotTargetStandabilityInspector.inspect(
        store.getExternalData().getWorld(),
        blockTarget,
        Vec3d.from(transform.getPosition()),
        this.useCenterOfBlock);
    Vec3i walkableTarget = standabilityInspection.getBestStandableCandidate();
    if (walkableTarget == null) {
      return null;
    }

    return this.useCenterOfBlock
        ? Vec3d.from(Utils.centerOf(walkableTarget))
        : Vec3d.from(walkableTarget);
  }

  private void findSnapshotTarget(WorkplaceAreaSnapshot snapshot) {
    this.target = null;
    for (String blockId : this.blockIds) {
      BlockTarget target = snapshot.peekNextSnapshotTargetFrom(blockId);
      if (target != null) {
        this.target = target;
        return;
      }
    }
  }

}
