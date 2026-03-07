package com.pacificbytestudios.projectciviccore.utils;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.core.spawner.BakerySpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.FarmSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.MineSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.SawmillSpawner;
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
      case BAKERY -> BakerySpawner::spawn;
      case SAWMILL -> SawmillSpawner::spawn;
      default -> throw new IllegalArgumentException("Unsupported workplace type: " + workplaceType);
    };
  }

  public static DespawnFunction getDespawnFunction(WorkplaceType workplaceType) {
    return switch (workplaceType) {
      case MINE -> MineSpawner::despawn;
      case FARM -> FarmSpawner::despawn;
      case BAKERY -> BakerySpawner::despawn;
      case SAWMILL -> SawmillSpawner::despawn;
      default -> throw new IllegalArgumentException("Unsupported workplace type: " + workplaceType);
    };
  }

  public static WorldChunk setBlockChunkAware(WorldChunk chunk, Vector3i position, BlockType blockType) {
    if (ChunkUtil.isInsideChunk(chunk.getX(), chunk.getZ(), position.x, position.z)) {
      chunk.setBlock(position.x, position.y, position.z, blockType);
      return chunk;
    }
    WorldChunk correctChunk = chunk.getWorld().getChunk(ChunkUtil.indexChunkFromBlock(position.x, position.z));
    correctChunk.setBlock(position.x, position.y, position.z, blockType);
    return correctChunk;
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
}
