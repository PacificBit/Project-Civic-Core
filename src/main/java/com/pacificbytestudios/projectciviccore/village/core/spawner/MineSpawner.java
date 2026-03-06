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

public class MineSpawner {
  private static final String PREFAB_JSON = "Structures/WorkPlace/Workspace_Mine.prefab.json";
  private static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Mine_Core");

  public static boolean spawn(WorldChunk chunk, Vector3i position) {
    World world = chunk.getWorld();
    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        PREFAB_JSON,
        position.clone(),
        Rotation.None,
        true);
    if (trackedPlacement == null) {
      return false;
    }

    chunk.setBlock(position.x, position.y + 1, position.z, WORKPLACE_BLOCK);
    Ref<ChunkStore> workplace = chunk.getBlockComponentEntity(position.x,
        position.y + 1, position.z);

    if (workplace == null || !workplace.isValid()) {
      chunk.breakBlock(position.x, position.y + 1, position.z);
      PrefabManager.restoreTrackedPrefab(world, trackedPlacement);
      return false;
    }

    workplace.getStore().putComponent(
        workplace,
        TrackedPrefabPlacementComponent.getComponentType(),
        trackedPlacement);
    return true;
  }

  public static boolean despawn(WorldChunk chunk, Vector3i position) {
    return PrefabManager.removePrefab(
        chunk.getWorld(),
        PREFAB_JSON,
        position.clone(),
        Rotation.None,
        true);
  }
}
