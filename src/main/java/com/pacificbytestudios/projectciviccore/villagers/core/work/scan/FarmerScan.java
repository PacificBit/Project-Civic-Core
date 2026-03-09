package com.pacificbytestudios.projectciviccore.villagers.core.work.scan;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;

public class FarmerScan implements IWorkAreaScanner {
  private static final String EMPTY_BLOCK_ID = "Empty";
  private static final long MAX_TIME_NS = 1_000_000;

  @Override
  public WorkplaceAreaSnapshot execute(
      Store<EntityStore> store,
      Ref<EntityStore> villagerRef,
      UUID uuid,
      VillagerComponent villager) {
    ProjectCivicCore.LOGGER.atInfo().log("[FarmerScan] Starting new farm scan at workplace: " +
        villager.getWorkerData().getWorkplace().getBenchPos());

    WorkplaceAreaSnapshot snapshotInProgress = villager.getWorkerData().getWorkareaSnapshot();

    if (snapshotInProgress == null || snapshotInProgress.isCompleted()) {
      snapshotInProgress = new WorkplaceAreaSnapshot();
    }

    WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
    if (snapshotInProgress.isCompleted() && !snapshotInProgress.isExpired(worldTimeResource)) {
      return snapshotInProgress;
    }

    ClaimedAreaComponent workingArea = villager.getWorkerArea(uuid);

    Vector2i topLeftCorner = workingArea.getTopLeft();
    Vector2i botRightCorner = workingArea.getBotRight();
    int soilLevel = workingArea.getY();
    int upperLevel = soilLevel + 1;

    Vector3i currentPoint = snapshotInProgress.getLastVisited();
    if (currentPoint == null) {
      currentPoint = new Vector3i(topLeftCorner.x, soilLevel, topLeftCorner.y);
    }

    World world = villager.getWorkerData()
        .getWorkplace()
        .getVillageHeartRef()
        .getStore()
        .getExternalData()
        .getWorld();

    Map<Long, WorldChunk> chunkCache = new HashMap<>();

    long initialChunkIndex = ChunkUtil.indexChunkFromBlock(currentPoint.x, currentPoint.z);
    WorldChunk chunk = getOrLoadChunk(world, chunkCache, initialChunkIndex);

    long start = System.nanoTime();

    while (isInsideWorkArea(currentPoint, topLeftCorner, botRightCorner, soilLevel, upperLevel)) {
      if (System.nanoTime() - start >= MAX_TIME_NS) {
        snapshotInProgress.setLastVisited(new Vector3i(currentPoint.x, currentPoint.y, currentPoint.z));
        return snapshotInProgress;
      }

      chunk = getCorrectChunkForBlock(currentPoint, world, chunk, chunkCache);

      BlockType type = chunk.getBlockType(currentPoint);

      if (!type.getId().equals(EMPTY_BLOCK_ID)) {
        ArrayDeque<Vector3i> found = snapshotInProgress.getBlockTypePosition()
            .computeIfAbsent(type.getId(), k -> new ArrayDeque<>());

        found.addLast(new Vector3i(currentPoint.x, currentPoint.y, currentPoint.z));
      }

      Vector3i nextPoint = getNextPoint(currentPoint, topLeftCorner, botRightCorner, soilLevel, upperLevel);

      if (nextPoint == null) {
        snapshotInProgress.setLastVisited(new Vector3i(currentPoint.x, currentPoint.y, currentPoint.z));
        snapshotInProgress.captureNow(worldTimeResource);
        snapshotInProgress.setCompleted(true);
        return snapshotInProgress;
      }

      currentPoint = nextPoint;
      snapshotInProgress.setLastVisited(new Vector3i(currentPoint.x, currentPoint.y, currentPoint.z));
    }

    snapshotInProgress.captureNow(worldTimeResource);
    snapshotInProgress.setCompleted(true);
    return snapshotInProgress;
  }

  private boolean isInsideWorkArea(
      Vector3i point,
      Vector2i topLeftCorner,
      Vector2i botRightCorner,
      int soilLevel,
      int upperLevel) {
    return point.x >= topLeftCorner.x
        && point.x <= botRightCorner.x
        && point.z >= topLeftCorner.y
        && point.z <= botRightCorner.y
        && point.y >= soilLevel
        && point.y <= upperLevel;
  }

  private Vector3i getNextPoint(
      Vector3i currentPoint,
      Vector2i topLeftCorner,
      Vector2i botRightCorner,
      int soilLevel,
      int upperLevel) {

    if (currentPoint.y == soilLevel) {
      return new Vector3i(currentPoint.x, upperLevel, currentPoint.z);
    }

    if (currentPoint.x < botRightCorner.x) {
      return new Vector3i(currentPoint.x + 1, soilLevel, currentPoint.z);
    }

    if (currentPoint.z < botRightCorner.y) {
      return new Vector3i(topLeftCorner.x, soilLevel, currentPoint.z + 1);
    }

    return null;
  }

  private WorldChunk getCorrectChunkForBlock(
      Vector3i currentPoint,
      World world,
      WorldChunk chunk,
      Map<Long, WorldChunk> chunkCache) {

    if (!ChunkUtil.isInsideChunk(chunk.getX(), chunk.getZ(), currentPoint.x, currentPoint.z)) {
      long chunkIndex = ChunkUtil.indexChunkFromBlock(currentPoint.x, currentPoint.z);
      return getOrLoadChunk(world, chunkCache, chunkIndex);
    }

    return chunk;
  }

  private WorldChunk getOrLoadChunk(World world, Map<Long, WorldChunk> chunkCache, long chunkIndex) {
    WorldChunk chunk = chunkCache.get(chunkIndex);
    if (chunk == null) {
      chunk = world.getChunk(chunkIndex);
      chunkCache.put(chunkIndex, chunk);
    }
    return chunk;
  }
}
