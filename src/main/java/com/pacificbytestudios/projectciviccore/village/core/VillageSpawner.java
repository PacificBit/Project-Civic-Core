package com.pacificbytestudios.projectciviccore.village.core;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

public class VillageSpawner {
  private static final String TOWN_CORE_BLOCK = "Village_Core";
  private static final BlockType TOWN_CORE = BlockType.getAssetMap().getAsset(TOWN_CORE_BLOCK);

  public static void spawn(WorldChunk chunk, Vector3i position) {
    chunk.setBlock(position.x, position.y, position.z, TOWN_CORE);
  }
}
