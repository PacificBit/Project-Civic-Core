package com.pacificbytestudios.projectciviccore.villagers.core.work.scan;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.builtin.adventure.farming.states.TilledSoilBlock;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.HarvestingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.FarmingData;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.AreaFootprint;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.ScanUtils;

public class FarmerScan implements IWorkAreaScanner {
  private static final String SOIL_GRASS_ID = "Soil_Grass";
  private static final String SOIL_MUD_DRY_ID = "Soil_Mud_Dry";
  public static final String TILLED_SOIL_ID = "Soil_Dirt_Tilled";

  public static final String TILLABLE_TERRAIN = "Tillable_Terrain";
  public static final String WATERABLE_TERRAIN = "Waterable_Terrain";
  public static final String PLANTABLE_TERRAIN = "Plantable_Terrain";
  public static final String HARVESTABLE_CROP = "Harvestable_Crop";

  private static final String STATE_DEFINITIONS_MARKER = "_State_Definitions_";
  private static final String FINAL_STAGE_MARKER = "StageFinal";

  private static final long MAX_TIME_NS = 1_000_000;

  private final Map<Long, WorldChunk> chunkCache = new HashMap<>();

  private World contextWorld;
  private Store<ChunkStore> chunkStore;

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

    if (snapshotInProgress != null && snapshotInProgress.isValid(worldTimeResource)) {
      return snapshotInProgress;
    }

    if (snapshotInProgress == null || snapshotInProgress.isCompleted()) {
      snapshotInProgress = new WorkplaceAreaSnapshot();
    }

    AreaFootprint workingArea = villager.getWorkerArea(uuid);

    int soilLevel = workingArea.getY();

    int startX = workingArea.getMinX();
    int endX = workingArea.getMaxX();
    int startZ = workingArea.getMinZ();
    int endZ = workingArea.getMaxZ();

    Vec3i currentPoint = snapshotInProgress.getLastVisited();
    if (currentPoint == null || !ScanUtils.isInsideWorkArea(currentPoint, startX, endX, startZ, endZ, soilLevel)) {
      currentPoint = new Vec3i(startX, soilLevel, startZ);
    } else {
      currentPoint = new Vec3i(
          ScanUtils.clamp(currentPoint.x, startX, endX),
          soilLevel,
          ScanUtils.clamp(currentPoint.z, startZ, endZ));
    }

    this.contextWorld = store.getExternalData().getWorld();
    this.chunkStore = this.contextWorld.getChunkStore().getStore();

    long initialChunkIndex = ChunkUtil.indexChunkFromBlock(currentPoint.x, currentPoint.z);
    WorldChunk soilChunk = getOrLoadChunk(initialChunkIndex);

    if (soilChunk == null) {
      Logger.logWarning(
          "Initial chunk is null for point " + currentPoint);
      return snapshotInProgress;
    }

    long start = System.nanoTime();
    int scannedColumns = 0;

    while (ScanUtils.isInsideWorkArea(currentPoint, startX, endX, startZ, endZ, soilLevel)) {
      if (System.nanoTime() - start >= MAX_TIME_NS) {
        snapshotInProgress.setLastVisited(Vec3i.from(currentPoint));
        Logger.logInfo(
            "Scan budget exceeded after " + scannedColumns
                + " columns, continuing next tick from " + currentPoint);
        return snapshotInProgress;
      }

      soilChunk = getCorrectChunkForBlock(currentPoint, soilChunk);
      if (soilChunk == null) {
        Logger.logWarning(
            "Chunk is null while scanning point " + currentPoint);
        break;
      }

      scanColumnTasks(snapshotInProgress, soilChunk, currentPoint);
      scannedColumns++;

      Vec3i nextPoint = ScanUtils.getNextPoint(currentPoint, startX, endX, startZ, endZ, soilLevel);
      if (nextPoint == null) {
        break;
      }

      currentPoint = nextPoint;
      snapshotInProgress.setLastVisited(Vec3i.from(currentPoint));
    }

    snapshotInProgress.captureNow(worldTimeResource);
    snapshotInProgress.setCompleted(true);

    return snapshotInProgress;
  }

  private void scanColumnTasks(
      WorkplaceAreaSnapshot snapshotInProgress,
      WorldChunk soilChunk,
      Vec3i soilPosition) {

    BlockType soilType = soilChunk.getBlockType(soilPosition);
    String soilBlockId = ScanUtils.getBlockId(soilType);

    Vec3i abovePosition = new Vec3i(
        soilPosition.x,
        soilPosition.y + 1,
        soilPosition.z);

    BlockType aboveType = soilChunk != null ? soilChunk.getBlockType(abovePosition) : null;

    if (ScanUtils.matchesBaseId(soilBlockId, SOIL_GRASS_ID) || ScanUtils.matchesBaseId(soilBlockId, SOIL_MUD_DRY_ID)) {
      snapshotInProgress.addWorkplaceSnapshotTarget(TILLABLE_TERRAIN, soilPosition);
      return;
    }

    if (!ScanUtils.matchesBaseId(soilBlockId, TILLED_SOIL_ID)) {
      return;
    }

    if (isHarvestableCropBlock(aboveType)) {
      snapshotInProgress.addWorkplaceSnapshotTarget(HARVESTABLE_CROP, abovePosition);
      return;
    }

    if (!isTilledSoilWatered(soilChunk, soilPosition)) {
      snapshotInProgress.addWorkplaceSnapshotTarget(WATERABLE_TERRAIN, soilPosition);
    }

    if (ScanUtils.isEmptyBlock(aboveType)) {
      snapshotInProgress.addWorkplaceSnapshotTarget(PLANTABLE_TERRAIN, abovePosition);
    }
  }

  private boolean isHarvestableCropBlock(BlockType blockType) {
    if (blockType == null) {
      return false;
    }

    String blockId = ScanUtils.getBlockId(blockType);

    if (isFinalCropStageId(blockId)) {
      return true;
    }

    FarmingData farmingData = blockType.getFarming();
    if (farmingData == null || farmingData.getStages() == null) {
      return false;
    }

    BlockGathering gathering = blockType.getGathering();
    if (gathering == null) {
      return false;
    }

    HarvestingDropType harvest = gathering.getHarvest();
    return harvest != null;
  }

  private WorldChunk getCorrectChunkForBlock(Vec3i currentPoint, WorldChunk chunk) {
    if (chunk == null || !ChunkUtil.isInsideChunk(
        chunk.getX(),
        chunk.getZ(),
        currentPoint.x,
        currentPoint.z)) {
      long chunkIndex = ChunkUtil.indexChunkFromBlock(currentPoint.x,
          currentPoint.z);
      return getOrLoadChunk(chunkIndex);
    }

    return chunk;
  }

  private WorldChunk getOrLoadChunk(long chunkIndex) {
    WorldChunk chunk = this.chunkCache.get(chunkIndex);
    if (chunk == null) {
      chunk = this.contextWorld.getChunk(chunkIndex);
      if (chunk != null) {
        this.chunkCache.put(chunkIndex, chunk);
      }
    }
    return chunk;
  }

  private boolean isTilledSoilWatered(WorldChunk chunk, Vec3i position) {
    if (chunk == null) {
      return false;
    }

    Ref<ChunkStore> blockEntityRef = chunk.getBlockComponentEntity(
        position.x, position.y, position.z);

    if (blockEntityRef == null) {
      return false;
    }

    TilledSoilBlock tilledSoilBlock = this.chunkStore.getComponent(
        blockEntityRef,
        TilledSoilBlock.getComponentType());

    if (tilledSoilBlock == null) {
      return false;
    }

    return tilledSoilBlock.getWateredUntil() != null;
  }

  private boolean isFinalCropStageId(String blockId) {
    if (blockId == null) {
      return false;
    }
    return blockId.contains(STATE_DEFINITIONS_MARKER) && blockId.contains(FINAL_STAGE_MARKER);
  }

}
