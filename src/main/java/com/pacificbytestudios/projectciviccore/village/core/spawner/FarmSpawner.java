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
import com.pacificbytestudios.projectciviccore.codecs.AreaFootprint;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerEntryComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerRegistryComponent;
import com.pacificbytestudios.projectciviccore.villagers.enums.WorkType;

import io.sentry.util.Pair;

public class FarmSpawner {
  public static boolean spawn(WorldChunk chunk, Vector3i position, VillageHeartComponent heart) {
    return spawn(chunk, position, Rotation.None, heart);
  }

  public static boolean spawn(
      WorldChunk chunk,
      Vector3i position,
      Rotation rotation,
      VillageHeartComponent heart) {
    Rotation structureRotation = StructurePlacementUtils.normalize(rotation);
    World world = chunk.getWorld();
    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        StructureConfig.Farm.PREFAB_JSON,
        position.clone(),
        structureRotation,
        true);
    if (trackedPlacement == null) {
      return false;
    }

    Vector3i workplaceBlockPosition = getWorkplaceBlockPosition(position, structureRotation);
    WorldChunk workplaceChunk = Utils.setBlockChunkAware(
        chunk,
        workplaceBlockPosition,
        StructureConfig.Farm.WORKPLACE_BLOCK,
        StructurePlacementUtils.toBlockRotationIndex(structureRotation));
    Ref<ChunkStore> workplace = workplaceChunk.getBlockComponentEntity(
        workplaceBlockPosition.x,
        workplaceBlockPosition.y,
        workplaceBlockPosition.z);

    if (workplace == null || !workplace.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[FarmSpawner - spawn] Workplace ref is invalid");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement, workplaceBlockPosition, null);
    }

    Store<ChunkStore> chunkStore = workplace.getStore();
    WorkplaceComponent workplaceComponent = chunkStore.getComponent(
        workplace,
        WorkplaceComponent.getComponentType());

    if (workplaceComponent == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[FarmSpawner - spawn] Workplace component is missing");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement, workplaceBlockPosition, null);
    }

    if (workplaceComponent.getUuid() == null) {
      workplaceComponent.setUuid(UUID.randomUUID());
    }

    workplaceComponent.setLevel((short) 1);
    workplaceComponent.setBenchPos(workplaceBlockPosition);
    workplaceComponent.setBenchAccessPos(getBenchAccessPosition(workplaceBlockPosition, structureRotation));
    workplaceComponent.setWorkerType(new WorkType[] { WorkType.FARMER });
    workplaceComponent.setWorkingArea(new AreaFootprint(
        StructureConfig.Farm.WORKING_AREA_OFFSET,
        position,
        structureRotation));
    workplaceComponent.setWorkplaceRef(new EntityRef(chunk.getWorld().getName(), workplaceBlockPosition));

    chunkStore.putComponent(
        workplace,
        TrackedPrefabPlacementComponent.getComponentType(),
        trackedPlacement);

    Vector3i containerPosition = getContainerPosition(workplaceBlockPosition, structureRotation);
    WorldChunk containerChunk = Utils.setBlockChunkAware(
        workplaceChunk,
        containerPosition,
        StructureConfig.Farm.SINGLE_STORAGE_BLOCK,
        StructurePlacementUtils.toBlockRotationIndex(structureRotation));
    Ref<ChunkStore> containerRef = containerChunk.getBlockComponentEntity(containerPosition.x,
        containerPosition.y, containerPosition.z);

    if (containerRef == null || !containerRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[FarmSpawner - spawn] Container ref is invalid");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement, workplaceBlockPosition, containerPosition);
    }

    WorkplaceContainerEntryComponent container = chunkStore.getComponent(
        containerRef,
        WorkplaceContainerEntryComponent.getComponentType());

    if (container == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[FarmSpawner - spawn] Container component is missing");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement, workplaceBlockPosition, containerPosition);
    }

    workplaceComponent.setPrefabAnchor(position.clone());

    container.setPosition(containerPosition);
    container.setAccessPosition(getContainerAccessPosition(containerPosition, structureRotation));
    container.setUuid(UUID.randomUUID());

    WorkplaceContainerRegistryComponent storage = new WorkplaceContainerRegistryComponent();
    storage.registerContainer(container.getUuid(), container);
    workplaceComponent.setStorage(storage);

    chunk.markNeedsSaving();
    return true;
  }

  public static boolean despawn(WorldChunk chunk, Vector3i position) {
    Vector3i workplaceBlockPosition = position.clone();
    Pair<WorldChunk, Ref<ChunkStore>> result = Utils.getBlockComponentEntityChunkAware(
        chunk,
        workplaceBlockPosition);

    WorldChunk workplaceChunk = result.getFirst();
    Ref<ChunkStore> workplaceRef = result.getSecond();

    if (workplaceRef == null || !workplaceRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[FarmSpawner - despawn] Invalid workplaceRef");
      return false;
    }

    Store<ChunkStore> store = workplaceRef.getStore();

    WorkplaceComponent component = store.getComponent(workplaceRef,
        WorkplaceComponent.getComponentType());
    TrackedPrefabPlacementComponent trackedPlacement = store
        .getComponent(workplaceRef, TrackedPrefabPlacementComponent.getComponentType());

    if (component == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[FarmSpawner - despawn] Missing workplace component");
      return false;
    }

    Rotation prefabRotation = trackedPlacement != null ? trackedPlacement.getYaw() : Rotation.None;
    boolean despawn = false;
    if (trackedPlacement != null && trackedPlacement.hasRestoreSnapshot()) {
      despawn = PrefabManager.restoreTrackedPrefab(workplaceChunk.getWorld(), trackedPlacement);
    } else {
      Vector3i prefabAnchor = component.getPrefabAnchor();
      if (prefabAnchor == null && trackedPlacement != null) {
        prefabAnchor = trackedPlacement.getOrigin();
      }
      if (prefabAnchor == null) {
        prefabAnchor = getPrefabAnchorFromWorkplace(workplaceBlockPosition, prefabRotation);
      }

      despawn = PrefabManager.removePrefab(
          workplaceChunk.getWorld(),
          StructureConfig.Farm.PREFAB_JSON,
          prefabAnchor,
          prefabRotation,
          true);
    }

    if (!despawn) {
      ProjectCivicCore.LOGGER.atSevere().log("[FarmSpawner - despawn] Could not despawn farm prefab");
      return false;
    }

    Vector3i containerPosition = getContainerPosition(workplaceBlockPosition, prefabRotation);
    Utils.breakBlockChunkAware(workplaceChunk, workplaceBlockPosition);
    Utils.breakBlockChunkAware(workplaceChunk, containerPosition);

    return despawn;
  }

  private static Vector3i getWorkplaceBlockPosition(Vector3i prefabAnchor, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        prefabAnchor,
        StructureConfig.Farm.WORKPLACE_BLOCK_OFFSET,
        rotation);
  }

  private static Vector3i getContainerPosition(Vector3i workplaceBlockPosition, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        workplaceBlockPosition,
        StructureConfig.Farm.CONTAINER_OFFSET,
        rotation);
  }

  private static Vector3i getBenchAccessPosition(Vector3i workplaceBlockPosition, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        workplaceBlockPosition,
        StructureConfig.Farm.BENCH_ACCESS_OFFSET,
        rotation);
  }

  private static Vector3i getContainerAccessPosition(Vector3i containerPosition, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        containerPosition,
        StructureConfig.Farm.CONTAINER_ACCESS_OFFSET,
        rotation);
  }

  private static Vector3i getPrefabAnchorFromWorkplace(Vector3i workplaceBlockPosition, Rotation rotation) {
    return StructurePlacementUtils.removeOffset(
        workplaceBlockPosition,
        StructureConfig.Farm.WORKPLACE_BLOCK_OFFSET,
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
