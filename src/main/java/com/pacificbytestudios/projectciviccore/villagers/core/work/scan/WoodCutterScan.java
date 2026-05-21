package com.pacificbytestudios.projectciviccore.villagers.core.work.scan;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3i;

import com.hypixel.hytale.builtin.adventure.farming.states.FarmingBlock;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.AreaFootprint;
import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.ScanUtils;

public class WoodCutterScan implements IWorkAreaScanner {
  private static final long MAX_TIME_NS = 1_000_000;
  private static final int SCAN_STEP = 4;

  private static final Set<String> CLUTTER = Set.of(
      "Wood_Sticks",
      "Furniture_Desert_Torch",
      "Plant_Grass_Lush",
      "Plant_Grass_Lush_Short",
      "Rubble_Quartzite");

  public static final String CUTTABLE_TREE = "Cuttable_Tree";
  public static final String CUTTABLE_BLOCK = "Cuttable_Block";
  public static final String PLANTABLE_TERRAIN = "Plantable_Terrain";

  private final Map<Long, WorldChunk> chunkCache = new HashMap<>();
  private World contextWorld;

  @Override
  public WorkplaceAreaSnapshot execute(
      Store<EntityStore> store,
      Ref<EntityStore> villagerRef,
      UUID uuid,
      VillagerComponent villager) {

    ProjectCivicCore.LOGGER.atInfo().log("[WoodCutterScan] Starting new woodCutter scan");

    VillagerWorkerComponent worker = villager.getWorkerData();
    if (worker == null) {
      return null;
    }

    WorkplaceAreaSnapshot snapshotInProgress = worker.getWorkareaSnapshot();
    WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());

    if (snapshotInProgress != null &&
        snapshotInProgress.isValid(worldTimeResource)) {
      return snapshotInProgress;
    }

    if (snapshotInProgress == null || snapshotInProgress.isCompleted()) {
      snapshotInProgress = new WorkplaceAreaSnapshot();
    }

    AreaFootprint workingArea = villager.getWorkerArea(uuid);

    int aboveTerrainY = workingArea.getY() + 1;
    int startX = workingArea.getMinX();
    int endX = workingArea.getMaxX();
    int startZ = workingArea.getMinZ();
    int endZ = workingArea.getMaxZ();

    Vector3i currentPoint = getInitialScanPoint(
        snapshotInProgress.getLastVisited(),
        startX,
        endX,
        startZ,
        endZ,
        aboveTerrainY);

    if (currentPoint == null) {
      snapshotInProgress.captureNow(worldTimeResource);
      snapshotInProgress.setCompleted(true);
      return snapshotInProgress;
    }

    this.contextWorld = store.getExternalData().getWorld();

    long initialChunkIndex = ChunkUtil.indexChunkFromBlock(currentPoint.x,
        currentPoint.z);
    WorldChunk soilChunk = getOrLoadChunk(initialChunkIndex);

    long start = System.nanoTime();
    int scannedColumns = 0;

    while (currentPoint != null
        && ScanUtils.isInsideWorkArea(currentPoint, startX, endX, startZ, endZ,
            aboveTerrainY)) {

      if (System.nanoTime() - start >= MAX_TIME_NS) {
        snapshotInProgress.setLastVisited(VectorUtil.clone(currentPoint));
        ProjectCivicCore.LOGGER.atInfo().log(
            "[WoodCutterScan] Scan budget exceeded after " + scannedColumns
                + " columns, continuing next tick from " + currentPoint);
        return snapshotInProgress;
      }

      soilChunk = getCorrectChunkForBlock(currentPoint, soilChunk);

      if (soilChunk == null) {
        ProjectCivicCore.LOGGER.atSevere().log(
            "[WoodCutterScan] Chunk is null while scanning point " + currentPoint);
        break;
      }

      scanColumnTasks(snapshotInProgress, soilChunk, currentPoint);
      scannedColumns++;

      Vector3i nextPoint = getNextScanPoint(currentPoint, startX, endX, startZ,
          endZ, aboveTerrainY);
      if (nextPoint == null) {
        break;
      }

      currentPoint = nextPoint;
      snapshotInProgress.setLastVisited(VectorUtil.clone(currentPoint));
    }

    snapshotInProgress.captureNow(worldTimeResource);
    snapshotInProgress.setCompleted(true);
    return snapshotInProgress;
  }

  private void scanColumnTasks(
      WorkplaceAreaSnapshot snapshotInProgress,
      WorldChunk chunk,
      Vector3i aboveTerrainPosition) {

    BlockType aboveTerrainBlock = chunk.getBlockType(aboveTerrainPosition);

    if (ScanUtils.isEmptyBlock(aboveTerrainBlock) ||
        !isNotClutter(aboveTerrainBlock)) {
      addTask(snapshotInProgress, PLANTABLE_TERRAIN, aboveTerrainPosition);
      return;
    }

    FarmingBlock farming = BlockModule.getComponent(
        FarmingBlock.getComponentType(),
        this.contextWorld,
        aboveTerrainPosition.x,
        aboveTerrainPosition.y,
        aboveTerrainPosition.z);

    if (farming == null || farming.getGrowthProgress() >= 2.5f) {
      addTask(snapshotInProgress, CUTTABLE_TREE, aboveTerrainPosition);
      return;
    }

  }

  private Vector3i getInitialScanPoint(
      Vector3i lastVisited,
      int startX,
      int endX,
      int startZ,
      int endZ,
      int soilLevel) {

    if (lastVisited == null
        || !ScanUtils.isInsideWorkArea(lastVisited, startX, endX, startZ, endZ,
            soilLevel)) {
      return new Vector3i(startX, soilLevel, startZ);
    }

    Vector3i clampedPoint = new Vector3i(
        ScanUtils.clamp(lastVisited.x, startX, endX),
        soilLevel,
        ScanUtils.clamp(lastVisited.z, startZ, endZ));

    return snapToScanGrid(clampedPoint, startX, endX, startZ, endZ, soilLevel);
  }

  private Vector3i snapToScanGrid(
      Vector3i point,
      int startX,
      int endX,
      int startZ,
      int endZ,
      int soilLevel) {

    int snappedZ = snapUpToGrid(point.z, startZ);
    if (snappedZ > endZ) {
      return null;
    }

    int snappedX = (snappedZ != point.z)
        ? startX
        : snapUpToGrid(point.x, startX);

    if (snappedX > endX) {
      snappedX = startX;
      snappedZ += SCAN_STEP;

      if (snappedZ > endZ) {
        return null;
      }
    }

    return new Vector3i(snappedX, soilLevel, snappedZ);
  }

  private int snapUpToGrid(int value, int origin) {
    int relative = value - origin;

    if (relative <= 0) {
      return origin;
    }

    int remainder = relative % SCAN_STEP;
    if (remainder == 0) {
      return value;
    }

    return value + (SCAN_STEP - remainder);
  }

  private Vector3i getNextScanPoint(
      Vector3i currentPoint,
      int startX,
      int endX,
      int startZ,
      int endZ,
      int soilLevel) {

    Vector3i nextPoint = VectorUtil.clone(currentPoint).add(SCAN_STEP, 0, 0);
    if (nextPoint.x <= endX) {
      return nextPoint;
    }

    nextPoint.set(startX, soilLevel, currentPoint.z + SCAN_STEP);
    if (nextPoint.z <= endZ) {
      return nextPoint;
    }

    return null;
  }

  private boolean isNotClutter(BlockType blockType) {
    return blockType != null
        && !ScanUtils.isEmptyBlock(blockType)
        && !CLUTTER.contains(blockType.getId());
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

  private WorldChunk getCorrectChunkForBlock(Vector3i currentPoint, WorldChunk chunk) {
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

  private void addTask(
      WorkplaceAreaSnapshot snapshotInProgress,
      String taskId,
      Vector3i position) {
    ArrayDeque<Vector3i> found = snapshotInProgress.getBlockTypePosition()
        .computeIfAbsent(taskId, key -> new ArrayDeque<>());

    found.addLast(VectorUtil.clone(position));
  }
}
