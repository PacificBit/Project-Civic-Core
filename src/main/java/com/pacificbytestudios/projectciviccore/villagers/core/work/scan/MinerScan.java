package com.pacificbytestudios.projectciviccore.villagers.core.work.scan;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.util.BlockPlacementHelper;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.Mineshaft;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineshaftGallery;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineshaftLevel;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;

public class MinerScan implements IWorkAreaScanner {
  private static final long MAX_TIME_NS = 1_000_000;
  private static final int GALLERY_SECTION_DEPTH = StructureConfig.Mine.MineshaftGalleryConf.SIZE_2I.y;
  private static final int MIN_WORLD_Y = 0;
  private static final int MAX_WORLD_Y = 319;
  private static final int SHIFT_START_VERTICAL_TOLERANCE = 3;
  private static final Vec3i LEVEL_SHIFT_START_OFFSET = new Vec3i(-5, 0, 0);

  public static final String DIG_POSITIONS = "Dig_Positions";
  public static final String CURRENT_LEVEL_SHIFT_START_POSITION = "Current_Level_Shift_Start_Position";
  public static final String CURRENT_SECTION_PREFAB_ANCHOR = "Current_Section_Prefab_Anchor";

  private World world;
  private long start;

  @Override
  public WorkplaceAreaSnapshot execute(
      Store<EntityStore> store,
      Ref<EntityStore> villagerRef,
      UUID uuid,
      VillagerComponent villager) {
    VillagerWorkerComponent worker = villager.getWorkerData();
    if (worker == null) {
      return null;
    }

    WorkplaceAreaSnapshot snapshotInProgress = worker.getWorkareaSnapshot();
    WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());

    Ref<ChunkStore> workplaceRef = worker.getWorkplace().getWorkplaceRef().getRef();
    if (workplaceRef == null || !workplaceRef.isValid()) {
      Logger.logWarning("Could not fetch workplaceRef");
      return snapshotInProgress;
    }

    MineComponent mineComponent = this.getMineComponent(workplaceRef);
    if (mineComponent == null) {
      Logger.logInfo("Error during key info fetch");
      return snapshotInProgress;
    }

    MineshaftGallery gallery = this.getAllocatedGallery(mineComponent, uuid);
    if (gallery == null) {
      Logger.logInfo("Error during key info fetch");
      return snapshotInProgress;
    }

    Vec3i galleryDirection = gallery.getCardinalDirection();
    Vec3i galleryStartPosition = gallery.getStartPosition();
    Vec3i currentDigPosition = gallery.getLastDiggedPosition();
    if (galleryDirection == null || galleryStartPosition == null || currentDigPosition == null) {
      Logger.logWarning("Gallery is missing scan coordinates");
      return snapshotInProgress;
    }

    Mineshaft mineshaft = mineComponent.getMineshaft();
    Short galleryLevel = mineshaft == null ? null : mineshaft.getLevelNumberForGallery(gallery.getGalleryId());
    Rotation mineshaftRotation = mineshaft == null ? null : mineshaft.getRotation();
    Vec3i levelAnchor = mineshaft == null ? null : this.resolveLevelAnchor(mineshaft, gallery.getGalleryId());
    TransformComponent villagerTransform = store.getComponent(villagerRef, TransformComponent.getComponentType());
    boolean shouldPopulateLevelShift = this.shouldPopulateLevelShiftStartPosition(villagerTransform,
        currentDigPosition);
    Vec3i levelShiftStartPosition = shouldPopulateLevelShift
        ? this.resolveLevelShiftStartPosition(levelAnchor, galleryLevel, mineshaftRotation)
        : null;
    if (snapshotInProgress != null && snapshotInProgress.isValid(worldTimeResource)) {
      this.storeCurrentLevelShiftStartMetadata(snapshotInProgress, levelShiftStartPosition);
      return snapshotInProgress;
    }

    if (snapshotInProgress == null || snapshotInProgress.isCompleted()) {
      snapshotInProgress = new WorkplaceAreaSnapshot();
    }
    snapshotInProgress.setCompleted(false);
    this.storeCurrentLevelShiftStartMetadata(snapshotInProgress, levelShiftStartPosition);

    this.start = System.nanoTime();
    this.world = store.getExternalData().getWorld();

    Vec3i sectionStartPosition = this.resolveCurrentSectionStart(
        galleryStartPosition,
        currentDigPosition,
        galleryDirection);
    Vec3i endPosition = Vec3i.from(sectionStartPosition).add(
        Vec3i.from(galleryDirection).mul(Math.max(GALLERY_SECTION_DEPTH - 1, 0)));

    if (System.nanoTime() - start >= MAX_TIME_NS) {
      Logger.logInfo(
          "Scan budget exceeded after fetching base info");
      return snapshotInProgress;
    }

    if (!this.scanDigSection(snapshotInProgress, currentDigPosition, sectionStartPosition, endPosition,
        galleryDirection)) {
      return snapshotInProgress;
    }

    snapshotInProgress.captureNow(worldTimeResource);
    snapshotInProgress.setCompleted(true);
    return snapshotInProgress;
  }

  private MineComponent getMineComponent(Ref<ChunkStore> workplaceRef) {
    MineComponent mineComponent = workplaceRef.getStore().getComponent(
        workplaceRef,
        MineComponent.getComponentType());
    if (mineComponent == null) {
      Logger.logWarning("Could not fetch mine component");
      return null;
    }

    return mineComponent;
  }

  private MineshaftGallery getAllocatedGallery(
      MineComponent mineComponent,
      UUID villagerId) {
    MineshaftGallery gallery = mineComponent.getWorkerAllocatedGallery(villagerId);
    if (gallery == null) {
      Logger.logWarning("Could not fetch working gallery");
      return null;
    }

    return gallery;
  }

  private Vec3i resolveLevelShiftStartPosition(
      Vec3i levelAnchor,
      Short levelNumber,
      Rotation mineshaftRotation) {
    if (levelNumber == null || levelNumber.shortValue() <= 1) {
      return null;
    }

    if (levelAnchor == null || mineshaftRotation == null) {
      return null;
    }

    return StructurePlacementUtils.withOffset(
        levelAnchor,
        LEVEL_SHIFT_START_OFFSET,
        mineshaftRotation);
  }

  private Vec3i resolveLevelAnchor(Mineshaft mineshaft, UUID galleryId) {
    if (mineshaft == null || galleryId == null) {
      return null;
    }

    for (MineshaftLevel level : mineshaft.getMineshaftLevels()) {
      if (level == null || !level.getGalleries().containsKey(galleryId)) {
        continue;
      }

      Vec3i anchorPoint = level.getAnchorPoint();
      return Vec3i.from(anchorPoint);
    }

    return null;
  }

  private boolean shouldPopulateLevelShiftStartPosition(
      TransformComponent villagerTransform,
      Vec3i digPosition) {
    if (villagerTransform == null || villagerTransform.getPosition() == null || digPosition == null) {
      return true;
    }

    Vec3i villagerPosition = Vec3i.from(villagerTransform.getPosition());
    return Math.abs(villagerPosition.y - digPosition.y) > SHIFT_START_VERTICAL_TOLERANCE;
  }

  private boolean scanDigSection(
      WorkplaceAreaSnapshot snapshotInProgress,
      Vec3i digStartPosition,
      Vec3i sectionStartPosition,
      Vec3i endPosition,
      Vec3i galleryDirection) {
    Vec3i leftDir = Utils.rotateNormalizedVector(galleryDirection, Rotation.Ninety);
    Vec3i rightDir = Utils.rotateNormalizedVector(galleryDirection, Rotation.TwoSeventy);
    Vec3i[] supportOffsets = new Vec3i[] {
        leftDir,
        new Vec3i(),
        rightDir
    };

    Vec3i position;
    if (this.canResumeCurrentSection(snapshotInProgress, digStartPosition, endPosition, galleryDirection)) {
      position = Vec3i.from(snapshotInProgress.getLastVisited());
    } else {
      this.resetScanProgress(snapshotInProgress);
      position = Vec3i.from(digStartPosition);
      addTask(snapshotInProgress, DIG_POSITIONS, position);
      this.queueSupportRepairs(snapshotInProgress, position, supportOffsets);
      snapshotInProgress.setLastVisited(Vec3i.from(position));
    }

    this.storeCurrentSectionMetadata(snapshotInProgress, sectionStartPosition, endPosition, galleryDirection);

    int distance = this.getHorizontalDistance(position, endPosition);
    for (int i = 0; i < distance; i++) {
      if (System.nanoTime() - start >= MAX_TIME_NS) {
        Logger.logInfo(
            "Scan budget exceeded after '" + i + "' blocks while checking the terrain");
        return false;
      }

      position.add(galleryDirection);
      addTask(snapshotInProgress, DIG_POSITIONS, position);
      this.queueSupportRepairs(snapshotInProgress, position, supportOffsets);
      snapshotInProgress.setLastVisited(Vec3i.from(position));
    }

    return true;
  }

  private Vec3i resolveCurrentSectionStart(
      Vec3i galleryStartPosition,
      Vec3i currentDigPosition,
      Vec3i galleryDirection) {
    if (galleryStartPosition == null || currentDigPosition == null || galleryDirection == null) {
      return Vec3i.from(currentDigPosition);
    }

    int travelledDistance = this.getTravelledDistanceAlongDirection(galleryStartPosition, currentDigPosition,
        galleryDirection);
    if (travelledDistance <= 0) {
      return Vec3i.from(galleryStartPosition);
    }

    int sectionOffset = Math.floorDiv(travelledDistance, GALLERY_SECTION_DEPTH) * GALLERY_SECTION_DEPTH;
    return Vec3i.from(galleryStartPosition).add(Vec3i.from(galleryDirection).mul(sectionOffset));
  }

  private void resetScanProgress(WorkplaceAreaSnapshot snapshotInProgress) {
    snapshotInProgress.clearSnapshotTargets();
    snapshotInProgress.setLastVisited(null);
  }

  private boolean canResumeCurrentSection(
      WorkplaceAreaSnapshot snapshotInProgress,
      Vec3i startPosition,
      Vec3i endPosition,
      Vec3i galleryDirection) {
    Vec3i lastVisited = snapshotInProgress.getLastVisited();
    if (lastVisited == null || lastVisited.y != startPosition.y) {
      return false;
    }

    if (galleryDirection.x != 0) {
      if (lastVisited.z != startPosition.z) {
        return false;
      }

      int travelled = lastVisited.x - startPosition.x;
      int sectionLength = endPosition.x - startPosition.x;
      return Integer.signum(travelled) == Integer.signum(sectionLength) || travelled == 0
          ? Math.abs(travelled) <= Math.abs(sectionLength)
          : false;
    }

    if (lastVisited.x != startPosition.x) {
      return false;
    }

    int travelled = lastVisited.z - startPosition.z;
    int sectionLength = endPosition.z - startPosition.z;
    return Integer.signum(travelled) == Integer.signum(sectionLength) || travelled == 0
        ? Math.abs(travelled) <= Math.abs(sectionLength)
        : false;
  }

  private int getHorizontalDistance(Vec3i from, Vec3i to) {
    Vec3i diffVec = Vec3i.from(to).sub(from);
    return diffVec.x != 0 ? Math.abs(diffVec.x) : Math.abs(diffVec.z);
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

  private void queueSupportRepairs(
      WorkplaceAreaSnapshot snapshotInProgress,
      Vec3i galleryPosition,
      Vec3i[] supportOffsets) {
    for (Vec3i supportOffset : supportOffsets) {
      Vec3i supportPosition = Vec3i.from(galleryPosition).add(supportOffset);
      if (this.needsSupportRepair(supportPosition)) {
        addTask(snapshotInProgress, TERRAIN_BLOCK_TO_FILL, supportPosition);
      }
    }
  }

  private boolean needsSupportRepair(Vec3i supportPosition) {
    if (supportPosition.y < MIN_WORLD_Y || supportPosition.y > MAX_WORLD_Y) {
      return false;
    }

    WorldChunk chunk = Utils.getWorldChunk(this.world, supportPosition);
    if (chunk == null) {
      return false;
    }

    BlockType blockType = Utils.getBlockTypeChunkAware(this.world, chunk, supportPosition);
    if (blockType == null) {
      return true;
    }

    BlockSection blockSection = this.getBlockSection(supportPosition);
    if (blockSection == null) {
      return blockType.getMaterial() != BlockMaterial.Solid;
    }

    int rotation = blockSection.getRotationIndex(supportPosition.x, supportPosition.y, supportPosition.z);
    int filler = blockSection.getFiller(supportPosition.x, supportPosition.y, supportPosition.z);
    return !BlockPlacementHelper.testSupportingBlock(blockType, rotation, filler);
  }

  private BlockSection getBlockSection(Vec3i position) {
    if (position.y < MIN_WORLD_Y || position.y > MAX_WORLD_Y) {
      return null;
    }

    Ref<ChunkStore> sectionRef = this.world.getChunkStore().getChunkSectionReferenceAtBlock(
        position.x,
        position.y,
        position.z);
    if (sectionRef == null || !sectionRef.isValid()) {
      return null;
    }

    return sectionRef.getStore().getComponent(sectionRef, BlockSection.getComponentType());
  }

  private void storeCurrentSectionMetadata(
      WorkplaceAreaSnapshot snapshotInProgress,
      Vec3i startPosition,
      Vec3i endPosition,
      Vec3i galleryDirection) {
    Vec3i prefabAnchor = Vec3i.from(startPosition)
        .add(Vec3i.from(galleryDirection).mul(GALLERY_SECTION_DEPTH));
    this.addTask(snapshotInProgress, CURRENT_SECTION_PREFAB_ANCHOR, prefabAnchor);
  }

  private void storeCurrentLevelShiftStartMetadata(
      WorkplaceAreaSnapshot snapshotInProgress,
      Vec3i shiftStartPosition) {
    if (shiftStartPosition == null) {
      snapshotInProgress.removeSnapshotGroup(CURRENT_LEVEL_SHIFT_START_POSITION);
      return;
    }

    this.addTask(snapshotInProgress, CURRENT_LEVEL_SHIFT_START_POSITION, shiftStartPosition);
  }

  private void addTask(
      WorkplaceAreaSnapshot snapshotInProgress,
      String taskId,
      Vec3i position) {
    snapshotInProgress.addWorkplaceSnapshotTarget(taskId, Vec3i.from(position));
  }
}
