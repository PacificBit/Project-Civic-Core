package com.pacificbytestudios.projectciviccore.village.core;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.pacificbytestudios.projectciviccore.utils.PrefabSpawner;

public class VillageSpawner {
  private static final String TOWN_CORE_BLOCK = "Village_Core";
  private static final BlockType TOWN_CORE = BlockType.getAssetMap().getAsset(TOWN_CORE_BLOCK);

  public static boolean spawn(WorldChunk chunk, Vector3i position) {

    World world = chunk.getWorld();
    boolean spawned = PrefabSpawner.spawnAssetPrefab(
        chunk.getWorld(),
        world.getEntityStore().getStore(),
        "Structures/WorkPlace/Town_Hall.prefab.json",
        position.clone(),
        Rotation.None,
        true);

    chunk.setBlock(position.x, position.y + 1, position.z, TOWN_CORE);
    return spawned;
  }
}
