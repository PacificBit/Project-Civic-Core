package com.pacificbytestudios.projectciviccore.utils;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.core.spawner.FarmSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.MineSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.SawmillSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StorageSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.TavernSpawner;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceType;

import io.sentry.util.Pair;

public class Utils {

  public record BlockContext(WorldChunk chunk, Vector3i position) {
  }

  public static BlockContext getWorldBlockContext(
      Ref<ChunkStore> ref,
      CommandBuffer<ChunkStore> commandBuffer) {
    BlockModule.BlockStateInfo info = commandBuffer.getComponent(
        ref,
        BlockModule.BlockStateInfo.getComponentType());

    if (info == null) {
      return null;
    }

    int localX = ChunkUtil.xFromBlockInColumn(info.getIndex());
    int localY = ChunkUtil.yFromBlockInColumn(info.getIndex());
    int localZ = ChunkUtil.zFromBlockInColumn(info.getIndex());

    WorldChunk worldChunk = commandBuffer.getComponent(
        info.getChunkRef(),
        WorldChunk.getComponentType());

    if (worldChunk == null) {
      return null;
    }

    int worldX = ChunkUtil.worldCoordFromLocalCoord(worldChunk.getX(), localX);
    int worldY = localY;
    int worldZ = ChunkUtil.worldCoordFromLocalCoord(worldChunk.getZ(), localZ);

    return new BlockContext(worldChunk, new Vector3i(worldX, worldY, worldZ));
  }

  public static ClaimedAreaComponent claimVillageArea(VillageHeartComponent heart, Vector3i origin) {
    int level = heart.getLevel();
    int radiusPerLevel = heart.getConfig().getClaimRadiusPerLevel();
    int totalRadius = level * radiusPerLevel;
    Vector2i topLeft = new Vector2i(origin.x - totalRadius, origin.z - totalRadius);
    Vector2i botRight = new Vector2i(origin.x + totalRadius, origin.z + totalRadius);
    ProjectCivicCore.LOGGER.atInfo()
        .log("[claimVillageArea] Created new village with anchor points: " + topLeft + " and: " + botRight);
    ClaimedAreaComponent area = new ClaimedAreaComponent();
    area.setTopLeft(topLeft);
    area.setBotRight(botRight);
    heart.setClaimedArea(area);
    return area;
  }

  public static SpawnFunction getSpawnFunction(WorkplaceType workplaceType) {
    return switch (workplaceType) {
      case MINE -> MineSpawner::spawn;
      case FARM -> FarmSpawner::spawn;
      case TAVERN -> TavernSpawner::spawn;
      case SAWMILL -> SawmillSpawner::spawn;
      case STORAGE -> StorageSpawner::spawn;
      default -> throw new IllegalArgumentException("Unsupported workplace type: " + workplaceType);
    };
  }

  public static DespawnFunction getDespawnFunction(WorkplaceType workplaceType) {
    return switch (workplaceType) {
      case MINE -> MineSpawner::despawn;
      case FARM -> FarmSpawner::despawn;
      case TAVERN -> TavernSpawner::despawn;
      case SAWMILL -> SawmillSpawner::despawn;
      case STORAGE -> StorageSpawner::despawn;
      default -> throw new IllegalArgumentException("Unsupported workplace type: " + workplaceType);
    };
  }

  public static WorldChunk setBlockChunkAware(WorldChunk chunk, Vector3i position, BlockType blockType) {
    WorldChunk targetChunk = ChunkUtil.isInsideChunk(chunk.getX(), chunk.getZ(), position.x, position.z)
        ? chunk
        : chunk.getWorld().getChunk(ChunkUtil.indexChunkFromBlock(position.x, position.z));

    targetChunk.setBlock(position.x, position.y, position.z, blockType);
    return targetChunk;
  }

  public static WorldChunk setBlockChunkAware(WorldChunk chunk, Vector3i position, BlockType blockType, int rotation) {
    int blockId = BlockType.getAssetMap().getIndex(blockType.getId());

    WorldChunk targetChunk = ChunkUtil.isInsideChunk(chunk.getX(), chunk.getZ(), position.x, position.z)
        ? chunk
        : chunk.getWorld().getChunk(ChunkUtil.indexChunkFromBlock(position.x, position.z));

    targetChunk.setBlock(position.x, position.y, position.z, blockId, blockType,
        rotation, FillerBlockUtil.NO_FILLER, 0);

    return targetChunk;
  }

  public static WorldChunk breakBlockChunkAware(WorldChunk chunk, Vector3i position) {
    if (ChunkUtil.isInsideChunk(chunk.getX(), chunk.getZ(), position.x, position.z)) {
      chunk.breakBlock(position.x, position.y, position.z);
      return chunk;
    }

    WorldChunk correctChunk = chunk.getWorld().getChunk(ChunkUtil.indexChunkFromBlock(position.x, position.z));
    correctChunk.breakBlock(position.x, position.y, position.z);
    return correctChunk;
  }

  public static Pair<WorldChunk, Ref<ChunkStore>> getBlockComponentEntityChunkAware(WorldChunk chunk,
      Vector3i position) {
    if (ChunkUtil.isInsideChunk(chunk.getX(), chunk.getZ(), position.x, position.z)) {
      return new Pair<>(chunk, chunk.getBlockComponentEntity(position.x, position.y, position.z));
    }

    WorldChunk correctChunk = chunk.getWorld().getChunk(ChunkUtil.indexChunkFromBlock(position.x, position.z));
    return new Pair<>(correctChunk, correctChunk.getBlockComponentEntity(position.x, position.y, position.z));
  }

  public static void safeVillageTaskEnqueue(Ref<ChunkStore> villageRef, VillageTaskQueueComponent task) {
    Store<ChunkStore> chunkStore = villageRef.getStore();
    VillageTaskQueueComponent taskQueue = chunkStore.getComponent(villageRef,
        VillageTaskQueueComponent.getComponentType());

    if (taskQueue == null) {
      chunkStore.putComponent(
          villageRef,
          VillageTaskQueueComponent.getComponentType(),
          task);
      return;
    }

    taskQueue.merge(task);
  }

  public static ClaimedAreaComponent[] subdivideArea(ClaimedAreaComponent area, int parts) {
    if (parts <= 0) {
      throw new IllegalArgumentException("parts must be > 0");
    }

    if (parts == 1) {
      return new ClaimedAreaComponent[] { area };
    }

    Vector2i topLeft = area.getTopLeft();
    Vector2i botRight = area.getBotRight();

    int minX = Math.min(topLeft.x, botRight.x);
    int maxX = Math.max(topLeft.x, botRight.x);
    int minY = Math.min(topLeft.y, botRight.y);
    int maxY = Math.max(topLeft.y, botRight.y);

    int width = maxX - minX + 1;
    int height = maxY - minY + 1;

    boolean splitHorizontally = width >= height;
    int totalSize = splitHorizontally ? width : height;

    int baseSize = totalSize / parts;
    int remainder = totalSize % parts;
    int current = splitHorizontally ? minX : minY;

    ClaimedAreaComponent[] result = new ClaimedAreaComponent[parts];

    for (int i = 0; i < parts; i++) {
      int currentSize = baseSize + (i < remainder ? 1 : 0);
      int start = current;
      int end = start + currentSize - 1;

      result[i] = splitHorizontally
          ? new ClaimedAreaComponent(
              new Vector2i(start, minY),
              new Vector2i(end, maxY))
          : new ClaimedAreaComponent(
              new Vector2i(minX, start),
              new Vector2i(maxX, end));

      current = end + 1;
    }

    return result;
  }

  public static String safeToString(Object value) {
    return value == null ? "null" : value.toString();
  }

  @Nonnull
  public static Vector3f centerOf(@Nonnull Vector3i blockPos) {
    return new Vector3f(
        blockPos.x + 0.5f,
        blockPos.y + 0.5f,
        blockPos.z + 0.5f);
  }
}
