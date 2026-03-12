package com.pacificbytestudios.projectciviccore.villagers.core.work.scan;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.builtin.adventure.farming.states.TilledSoilBlock;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.HarvestingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.FarmingData;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;

public class FarmerScan implements IWorkAreaScanner {
  private static final String EMPTY_BLOCK_ID = "Empty";

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
    ProjectCivicCore.LOGGER.atInfo().log(
        "[FarmerScan] Starting new farmer scan");

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

    ClaimedAreaComponent workingArea = villager.getWorkerArea(uuid);

    Vector2i topLeftCorner = workingArea.getTopLeft();
    Vector2i botRightCorner = workingArea.getBotRight();
    int soilLevel = workingArea.getY();

    int startX = topLeftCorner.x;
    int endX = botRightCorner.x;
    int startZ = topLeftCorner.y;
    int endZ = botRightCorner.y;

    Vector3i currentPoint = snapshotInProgress.getLastVisited();
    if (currentPoint == null || !isInsideWorkArea(currentPoint, startX, endX, startZ, endZ, soilLevel)) {
      currentPoint = new Vector3i(startX, soilLevel, startZ);
    } else {
      currentPoint = new Vector3i(
          clamp(currentPoint.x, startX, endX),
          soilLevel,
          clamp(currentPoint.z, startZ, endZ));
    }

    this.contextWorld = store.getExternalData().getWorld();
    this.chunkStore = this.contextWorld.getChunkStore().getStore();

    long initialChunkIndex = ChunkUtil.indexChunkFromBlock(currentPoint.x, currentPoint.z);
    WorldChunk soilChunk = getOrLoadChunk(initialChunkIndex);

    if (soilChunk == null) {
      ProjectCivicCore.LOGGER.atSevere().log(
          "[FarmerScan] Initial chunk is null for point " + currentPoint);
      return snapshotInProgress;
    }

    long start = System.nanoTime();
    int scannedColumns = 0;

    while (isInsideWorkArea(currentPoint, startX, endX, startZ, endZ, soilLevel)) {
      if (System.nanoTime() - start >= MAX_TIME_NS) {
        snapshotInProgress.setLastVisited(currentPoint.clone());
        ProjectCivicCore.LOGGER.atInfo().log(
            "[FarmerScan] Scan budget exceeded after " + scannedColumns
                + " columns, continuing next tick from " + currentPoint);
        return snapshotInProgress;
      }

      soilChunk = getCorrectChunkForBlock(currentPoint, soilChunk);
      if (soilChunk == null) {
        ProjectCivicCore.LOGGER.atSevere().log(
            "[FarmerScan] Chunk is null while scanning point " + currentPoint);
        break;
      }

      scanColumnTasks(snapshotInProgress, soilChunk, currentPoint);
      scannedColumns++;

      Vector3i nextPoint = getNextPoint(currentPoint, startX, endX, startZ, endZ, soilLevel);
      if (nextPoint == null) {
        break;
      }

      currentPoint = nextPoint;
      snapshotInProgress.setLastVisited(currentPoint.clone());
    }

    snapshotInProgress.captureNow(worldTimeResource);
    snapshotInProgress.setCompleted(true);

    return snapshotInProgress;
  }

  private void scanColumnTasks(
      WorkplaceAreaSnapshot snapshotInProgress,
      WorldChunk soilChunk,
      Vector3i soilPosition) {

    BlockType soilType = soilChunk.getBlockType(soilPosition);
    String soilBlockId = getBlockId(soilType);

    Vector3i abovePosition = new Vector3i(
        soilPosition.x,
        soilPosition.y + 1,
        soilPosition.z);

    WorldChunk aboveChunk = getCorrectChunkForBlock(abovePosition, soilChunk);
    BlockType aboveType = aboveChunk != null ? aboveChunk.getBlockType(abovePosition) : null;

    if (matchesBaseId(soilBlockId, SOIL_GRASS_ID) || matchesBaseId(soilBlockId, SOIL_MUD_DRY_ID)) {
      addTask(snapshotInProgress, TILLABLE_TERRAIN, soilPosition);
      return;
    }

    if (!matchesBaseId(soilBlockId, TILLED_SOIL_ID)) {
      return;
    }

    if (isHarvestableCropBlock(aboveType)) {
      addTask(snapshotInProgress, HARVESTABLE_CROP, abovePosition);
      return;
    }

    if (!isTilledSoilWatered(soilChunk, soilPosition)) {
      addTask(snapshotInProgress, WATERABLE_TERRAIN, soilPosition);
    }

    if (isEmptyBlock(aboveType)) {
      addTask(snapshotInProgress, PLANTABLE_TERRAIN, abovePosition);
    }
  }

  private boolean isHarvestableCropBlock(BlockType blockType) {
    if (blockType == null) {
      return false;
    }

    String blockId = getBlockId(blockType);

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

  private void addTask(
      WorkplaceAreaSnapshot snapshotInProgress,
      String taskId,
      Vector3i position) {
    ArrayDeque<Vector3i> found = snapshotInProgress.getBlockTypePosition()
        .computeIfAbsent(taskId, key -> new ArrayDeque<>());

    found.addLast(position.clone());
  }

  private boolean isInsideWorkArea(
      Vector3i point,
      int minX,
      int maxX,
      int minZ,
      int maxZ,
      int soilLevel) {
    return point.x >= minX
        && point.x <= maxX
        && point.z >= minZ
        && point.z <= maxZ
        && point.y == soilLevel;
  }

  private Vector3i getNextPoint(
      Vector3i currentPoint,
      int minX,
      int maxX,
      int minZ,
      int maxZ,
      int soilLevel) {

    if (currentPoint.x < maxX) {
      return new Vector3i(currentPoint.x + 1, soilLevel, currentPoint.z);
    }

    if (currentPoint.z < maxZ) {
      return new Vector3i(minX, soilLevel, currentPoint.z + 1);
    }

    return null;
  }

  private WorldChunk getCorrectChunkForBlock(Vector3i currentPoint, WorldChunk chunk) {
    if (chunk == null || !ChunkUtil.isInsideChunk(
        chunk.getX(),
        chunk.getZ(),
        currentPoint.x,
        currentPoint.z)) {
      long chunkIndex = ChunkUtil.indexChunkFromBlock(currentPoint.x, currentPoint.z);
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

  private boolean isTilledSoilWatered(WorldChunk chunk, Vector3i position) {
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

  private String getBlockId(BlockType blockType) {
    if (blockType == null || blockType.getId() == null) {
      return "<null>";
    }
    return blockType.getId();
  }

  private boolean isEmptyBlock(BlockType blockType) {
    return matchesBaseId(getBlockId(blockType), EMPTY_BLOCK_ID);
  }

  private boolean matchesBaseId(String actualId, String expectedBaseId) {
    String normalized = normalizeBlockId(actualId);
    return normalized != null && normalized.equals(expectedBaseId);
  }

  private boolean isFinalCropStageId(String blockId) {
    if (blockId == null) {
      return false;
    }
    return blockId.contains(STATE_DEFINITIONS_MARKER) && blockId.contains(FINAL_STAGE_MARKER);
  }

  private String normalizeBlockId(String blockId) {
    if (blockId == null || "<null>".equals(blockId)) {
      return null;
    }

    String normalized = blockId;

    while (normalized.startsWith("*")) {
      normalized = normalized.substring(1);
    }

    while (normalized.endsWith("*")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }

    int stateIndex = normalized.indexOf(STATE_DEFINITIONS_MARKER);
    if (stateIndex >= 0) {
      normalized = normalized.substring(0, stateIndex);
    }

    return normalized;
  }

  private int clamp(int value, int min, int max) {
    if (value < min) {
      return min;
    }
    if (value > max) {
      return max;
    }
    return value;
  }
}
