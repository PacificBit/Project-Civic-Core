package com.pacificbytestudios.projectciviccore.village.core.spawner;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.BlockContext;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;

public class TownHeartSpawner {

  public static boolean spawn(WorldChunk chunk, Vector3i position) {
    return spawn(chunk, position, Rotation.None);
  }

  public static boolean spawn(WorldChunk chunk, Vector3i position, Rotation rotation) {
    World world = chunk.getWorld();
    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        StructureConfig.TownHeart.PREFAB_JSON,
        position.clone(),
        StructurePlacementUtils.normalize(rotation),
        true);
    if (trackedPlacement == null) {
      return false;
    }

    chunk.setBlock(position.x, position.y + 1, position.z, StructureConfig.TownHeart.WORKPLACE_BLOCK);
    Ref<ChunkStore> heartRef = chunk.getBlockComponentEntity(position.x, position.y + 1, position.z);
    if (heartRef == null || !heartRef.isValid()) {
      chunk.breakBlock(position.x, position.y + 1, position.z);
      PrefabManager.restoreTrackedPrefab(world, trackedPlacement);
      return false;
    }

    VillageHeartComponent heartComponent = heartRef.getStore().getComponent(heartRef,
        VillageHeartComponent.getComponentType());

    if (heartComponent == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[TownHeartSpawner - spawn] Could not fetch VillageHeartComponent");
      chunk.breakBlock(position.x, position.y + 1, position.z);
      PrefabManager.restoreTrackedPrefab(world, trackedPlacement);
      return false;
    }

    String worldName = chunk.getWorld().getName();
    heartComponent.setBlockContext(new BlockContext(worldName, chunk.getIndex(), position.clone().add(Vector3i.UP)));

    heartRef.getStore().putComponent(
        heartRef,
        TrackedPrefabPlacementComponent.getComponentType(),
        trackedPlacement);
    return true;
  }

  public static boolean despawn(WorldChunk chunk, Vector3i position) {
    return despawn(chunk, position, Rotation.None);
  }

  public static boolean despawn(WorldChunk chunk, Vector3i position, Rotation rotation) {
    return PrefabManager.removePrefab(
        chunk.getWorld(),
        StructureConfig.TownHeart.PREFAB_JSON,
        position.clone(),
        StructurePlacementUtils.normalize(rotation),
        true);
  }
}
