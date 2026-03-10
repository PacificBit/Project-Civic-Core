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
    ProjectCivicCore.LOGGER.atInfo().log("[FarmerScan] Starting new farm scan at workplace: " +
        villager.getWorkerData().getWorkplace().getBenchPos());

    WorkplaceAreaSnapshot snapshotInProgress = villager.getWorkerData().getWorkareaSnapshot();
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

    Vector3i currentPoint = snapshotInProgress.getLastVisited();
    if (currentPoint == null) {
      currentPoint = new Vector3i(topLeftCorner.x, soilLevel, topLeftCorner.y);
    } else {
      currentPoint = new Vector3i(currentPoint.x, soilLevel, currentPoint.z);
    }

    this.contextWorld = villager.getWorkerData()
        .getWorkplace()
        .getVillageHeartRef()
        .getStore()
        .getExternalData()
        .getWorld();

    this.chunkStore = this.contextWorld.getChunkStore().getStore();

    long initialChunkIndex = ChunkUtil.indexChunkFromBlock(currentPoint.x, currentPoint.z);
    WorldChunk soilChunk = getOrLoadChunk(initialChunkIndex);

    long start = System.nanoTime();

    while (isInsideWorkArea(currentPoint, topLeftCorner, botRightCorner, soilLevel)) {
      if (System.nanoTime() - start >= MAX_TIME_NS) {
        snapshotInProgress.setLastVisited(currentPoint.clone());
        ProjectCivicCore.LOGGER.atInfo().log("Farming scan took too long, dividing it into multiple scans");
        return snapshotInProgress;
      }

      soilChunk = getCorrectChunkForBlock(currentPoint, soilChunk);
      scanColumnTasks(snapshotInProgress, soilChunk, currentPoint);

      Vector3i nextPoint = getNextPoint(currentPoint, topLeftCorner, botRightCorner, soilLevel);
      if (nextPoint == null) {
        break;
      }

      currentPoint = nextPoint;
      snapshotInProgress.setLastVisited(currentPoint.clone());
    }

    snapshotInProgress.captureNow(worldTimeResource);
    System.out.println(snapshotInProgress.getBlockTypePosition());
    snapshotInProgress.setCompleted(true);
    return snapshotInProgress;
  }

  private void scanColumnTasks(
      WorkplaceAreaSnapshot snapshotInProgress,
      WorldChunk soilChunk,
      Vector3i soilPosition) {
    BlockType soilType = soilChunk.getBlockType(soilPosition);
    String soilBlockId = soilType.getId();

    if (SOIL_GRASS_ID.equals(soilBlockId) || SOIL_MUD_DRY_ID.equals(soilBlockId)) {
      addTask(snapshotInProgress, TILLABLE_TERRAIN, soilPosition);
      return;
    }

    if (!TILLED_SOIL_ID.equals(soilBlockId)) {
      return;
    }

    Vector3i abovePosition = new Vector3i(soilPosition.x, soilPosition.y + 1, soilPosition.z);
    WorldChunk aboveChunk = getCorrectChunkForBlock(abovePosition, soilChunk);
    BlockType aboveType = aboveChunk.getBlockType(abovePosition);
    String aboveBlockId = aboveType.getId();

    if (isHarvestableCropBlock(aboveType)) {
      addTask(snapshotInProgress, HARVESTABLE_CROP, abovePosition);
      return;
    }

    if (!isTilledSoilWatered(soilChunk, soilPosition)) {
      addTask(snapshotInProgress, WATERABLE_TERRAIN, soilPosition);
    }

    if (EMPTY_BLOCK_ID.equals(aboveBlockId)) {
      addTask(snapshotInProgress, PLANTABLE_TERRAIN, abovePosition);
    }
  }

  private boolean isHarvestableCropBlock(BlockType blockType) {
    if (!this.contextWorld.getGameplayConfig().getWorldConfig().isBlockGatheringAllowed()) {
      return false;
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
      Vector2i topLeftCorner,
      Vector2i botRightCorner,
      int soilLevel) {
    return point.x >= topLeftCorner.x
        && point.x <= botRightCorner.x
        && point.z >= topLeftCorner.y
        && point.z <= botRightCorner.y
        && point.y == soilLevel;
  }

  private Vector3i getNextPoint(
      Vector3i currentPoint,
      Vector2i topLeftCorner,
      Vector2i botRightCorner,
      int soilLevel) {
    if (currentPoint.x < botRightCorner.x) {
      return new Vector3i(currentPoint.x + 1, soilLevel, currentPoint.z);
    }

    if (currentPoint.z < botRightCorner.y) {
      return new Vector3i(topLeftCorner.x, soilLevel, currentPoint.z + 1);
    }

    return null;
  }

  private WorldChunk getCorrectChunkForBlock(Vector3i currentPoint, WorldChunk chunk) {
    if (!ChunkUtil.isInsideChunk(chunk.getX(), chunk.getZ(), currentPoint.x, currentPoint.z)) {
      long chunkIndex = ChunkUtil.indexChunkFromBlock(currentPoint.x, currentPoint.z);
      return getOrLoadChunk(chunkIndex);
    }

    return chunk;
  }

  private WorldChunk getOrLoadChunk(long chunkIndex) {
    WorldChunk chunk = this.chunkCache.get(chunkIndex);
    if (chunk == null) {
      chunk = this.contextWorld.getChunk(chunkIndex);
      this.chunkCache.put(chunkIndex, chunk);
    }
    return chunk;
  }

  private boolean isTilledSoilWatered(WorldChunk chunk, Vector3i position) {
    Ref<ChunkStore> blockEntityRef = chunk.getBlockComponentEntity(position.x, position.y, position.z);
    if (blockEntityRef == null) {
      return false;
    }

    TilledSoilBlock tilledSoilBlock = this.chunkStore.getComponent(blockEntityRef, TilledSoilBlock.getComponentType());
    if (tilledSoilBlock == null) {
      return false;
    }

    return tilledSoilBlock.getWateredUntil() != null;
  }
}
