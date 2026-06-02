package com.pacificbytestudios.projectciviccore.villagers.core.utils;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import org.joml.Vector3i;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class WoodCutterTerrainRepair {
  private final BlockType[] terrainRepairBlockTypes;
  private static final int TERRAIN_REPAIR_RADIUS = 1;
  private static final int TERRAIN_REPAIR_DEPTH = 4;

  public WoodCutterTerrainRepair(String[] terrainRepairBlockTypes) {
    this.terrainRepairBlockTypes = this.resolveTerrainRepairBlocks(terrainRepairBlockTypes);
  }

  public void repairTerrainBelowSapling(World world, Vector3i saplingPosition) {
    if (this.terrainRepairBlockTypes.length == 0) {
      return;
    }

    for (int x = saplingPosition.x - TERRAIN_REPAIR_RADIUS; x <= saplingPosition.x + TERRAIN_REPAIR_RADIUS; x++) {
      for (int z = saplingPosition.z - TERRAIN_REPAIR_RADIUS; z <= saplingPosition.z + TERRAIN_REPAIR_RADIUS; z++) {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) {
          continue;
        }

        for (int depth = 1; depth <= TERRAIN_REPAIR_DEPTH; depth++) {
          int y = saplingPosition.y - depth;

          Vector3i terrainPosition = new Vector3i(x, y, z);
          BlockType existingBlock = Utils.getBlockTypeChunkAware(world, chunk, terrainPosition);
          if (existingBlock != null && !isRoot(existingBlock) && existingBlock.getMaterial() == BlockMaterial.Solid) {
            continue;
          }

          BlockType repairBlock = this.getRandomTerrainRepairBlock();
          if (repairBlock != null) {
            chunk.setBlock(x, y, z, repairBlock);
          }
        }
      }
    }
  }

  private boolean isRoot(BlockType blockType) {
    String blockId = blockType.getId();
    return blockId.startsWith("Wood_") && (blockId.endsWith("_Trunk") || blockId.endsWith("_Trunk_Full"));
  }

  private BlockType getRandomTerrainRepairBlock() {
    if (this.terrainRepairBlockTypes.length == 0) {
      return null;
    }

    int index = ThreadLocalRandom.current().nextInt(this.terrainRepairBlockTypes.length);
    return this.terrainRepairBlockTypes[index];
  }

  private BlockType[] resolveTerrainRepairBlocks(String[] terrainRepairBlockIds) {
    if (terrainRepairBlockIds == null || terrainRepairBlockIds.length == 0) {
      return new BlockType[0];
    }

    ArrayList<BlockType> resolvedBlocks = new ArrayList<>(terrainRepairBlockIds.length);
    for (String blockId : terrainRepairBlockIds) {
      BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
      if (blockType == null) {
        Logger.logWarning("Unknown terrain repair block id: " + blockId);
        continue;
      }
      resolvedBlocks.add(blockType);
    }

    return resolvedBlocks.toArray(new BlockType[0]);
  }
}
