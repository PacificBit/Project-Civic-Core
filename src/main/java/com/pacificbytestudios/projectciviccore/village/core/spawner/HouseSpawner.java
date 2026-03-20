package com.pacificbytestudios.projectciviccore.village.core.spawner;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.house.AppartmentComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.VillageBedComponent;

import io.sentry.util.Pair;

public class HouseSpawner {
  public static boolean spawn(WorldChunk chunk, Vector3i position) {
    return spawn(chunk, position, Rotation.None);
  }

  public static boolean spawn(WorldChunk chunk, Vector3i position, Rotation rotation) {
    Rotation structureRotation = StructurePlacementUtils.normalize(rotation);
    World world = chunk.getWorld();
    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        StructureConfig.House.PREFAB_JSON,
        position.clone(),
        structureRotation,
        true);
    if (trackedPlacement == null) {
      return false;
    }

    Vector3i houseCoreBlockPosition = getWorkplaceBlockPosition(position, structureRotation);
    WorldChunk houseChunk = Utils.setBlockChunkAware(
        chunk,
        houseCoreBlockPosition,
        StructureConfig.House.CORE_BLOCK);
    Ref<ChunkStore> houseCoreRef = houseChunk.getBlockComponentEntity(
        houseCoreBlockPosition.x,
        houseCoreBlockPosition.y,
        houseCoreBlockPosition.z);

    if (houseCoreRef == null || !houseCoreRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[House - spawn] House ref is invalid");
      return rollbackFailedSpawn(world, houseChunk, trackedPlacement, houseCoreBlockPosition, null);
    }

    Store<ChunkStore> chunkStore = houseCoreRef.getStore();

    HouseComponent house = chunkStore.getComponent(houseCoreRef, HouseComponent.getComponentType());
    if (house == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[House - spawn] Could not fetch house component");
      return rollbackFailedSpawn(world, houseChunk, trackedPlacement, houseCoreBlockPosition, null);
    }

    house.setLevel((short) 1);
    house.setUuid(UUID.randomUUID());
    house.setCoreBlockPosition(houseCoreBlockPosition);
    house.setPrefabAnchor(position.clone());
    house.setHouseRef(new EntityRef(chunk.getWorld().getName(), houseCoreBlockPosition));
    house.setAppartmentRegistry(StructureConfig.House.getAppartments(position, structureRotation, 1));

    spawnBeds(world, chunk, house);

    chunkStore.putComponent(
        houseCoreRef,
        TrackedPrefabPlacementComponent.getComponentType(),
        trackedPlacement);

    houseChunk.markNeedsSaving();
    return true;
  }

  private static void spawnBeds(World world, WorldChunk chunk, HouseComponent house) {
    for (AppartmentComponent appartment : house.getAppartmentRegistry().getAppartments().values()) {
      for (VillageBedComponent bed : appartment.getBeds().values()) {
        Utils.setBlockChunkAware(chunk, bed.getPosition(), StructureConfig.House.CRUDE_BED_BLOCK);
        Utils.markNeedSaving(world, bed.getPosition());
      }
    }
  }

  public static boolean despawn(WorldChunk chunk, Vector3i position) {
    Vector3i bedBlockPosition = position.clone();
    Pair<WorldChunk, Ref<ChunkStore>> result = Utils.getBlockComponentEntityChunkAware(
        chunk,
        bedBlockPosition);

    WorldChunk bedChunk = result.getFirst();
    Ref<ChunkStore> bedRef = result.getSecond();

    if (bedRef == null || !bedRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[House - despawn] Invalid workplaceRef");
      return false;
    }

    Store<ChunkStore> store = bedRef.getStore();

    HouseComponent component = store.getComponent(bedRef, HouseComponent.getComponentType());
    TrackedPrefabPlacementComponent trackedPlacement = store
        .getComponent(bedRef, TrackedPrefabPlacementComponent.getComponentType());

    Rotation prefabRotation = trackedPlacement != null ? trackedPlacement.getYaw() : Rotation.None;
    boolean despawn = false;
    if (trackedPlacement != null && trackedPlacement.hasRestoreSnapshot()) {
      despawn = PrefabManager.restoreTrackedPrefab(bedChunk.getWorld(), trackedPlacement);
    } else {
      Vector3i prefabAnchor = component.getPrefabAnchor();
      if (prefabAnchor == null && trackedPlacement != null) {
        prefabAnchor = trackedPlacement.getOrigin();
      }
      if (prefabAnchor == null) {
        prefabAnchor = getPrefabAnchorFromWorkplace(bedBlockPosition, prefabRotation);
      }

      despawn = PrefabManager.removePrefab(
          bedChunk.getWorld(),
          StructureConfig.House.PREFAB_JSON,
          prefabAnchor,
          prefabRotation,
          true);
    }

    if (!despawn) {
      ProjectCivicCore.LOGGER.atSevere().log("[House - despawn] Could not despawn farm prefab");
      return false;
    }

    Utils.breakBlockChunkAware(bedChunk, bedBlockPosition);
    return despawn;
  }

  private static Vector3i getWorkplaceBlockPosition(Vector3i prefabAnchor, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        prefabAnchor,
        StructureConfig.House.CORE_BLOCK_OFFSET,
        rotation);
  }

  private static Vector3i getPrefabAnchorFromWorkplace(Vector3i workplaceBlockPosition, Rotation rotation) {
    return StructurePlacementUtils.removeOffset(
        workplaceBlockPosition,
        StructureConfig.House.CORE_BLOCK_OFFSET,
        rotation);
  }

  private static boolean rollbackFailedSpawn(
      World world,
      WorldChunk chunk,
      TrackedPrefabPlacementComponent trackedPlacement,
      Vector3i workplaceBlockPosition,
      Vector3i containerPosition) {
    Utils.breakBlockChunkAware(chunk, workplaceBlockPosition);
    if (containerPosition != null) {
      Utils.breakBlockChunkAware(chunk, containerPosition);
    }
    PrefabManager.restoreTrackedPrefab(world, trackedPlacement);
    return false;
  }
}
