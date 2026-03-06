package com.pacificbytestudios.projectciviccore.village.core.spawner;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;

public class TownHallSpawner {
  private static final String TOWN_HALL_PREFAB_JSON = "Structures/WorkPlace/Town_Hall.prefab.json";
  private static final BlockType TOWN_CORE = BlockType.getAssetMap().getAsset("Village_Core");

  public static boolean spawn(WorldChunk chunk, Vector3i position) {
    World world = chunk.getWorld();
    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        TOWN_HALL_PREFAB_JSON,
        position.clone(),
        Rotation.None,
        true);
    if (trackedPlacement == null) {
      return false;
    }

    chunk.setBlock(position.x, position.y + 1, position.z, TOWN_CORE);
    Ref<ChunkStore> heartRef = chunk.getBlockComponentEntity(position.x, position.y + 1, position.z);
    if (heartRef == null || !heartRef.isValid()) {
      chunk.breakBlock(position.x, position.y + 1, position.z);
      PrefabManager.restoreTrackedPrefab(world, trackedPlacement);
      return false;
    }

    heartRef.getStore().putComponent(
        heartRef,
        TrackedPrefabPlacementComponent.getComponentType(),
        trackedPlacement);
    return true;
  }

  public static boolean despawn(WorldChunk chunk, Vector3i position) {
    return PrefabManager.removePrefab(
        chunk.getWorld(),
        TOWN_HALL_PREFAB_JSON,
        position.clone(),
        Rotation.None,
        true);
  }
}
