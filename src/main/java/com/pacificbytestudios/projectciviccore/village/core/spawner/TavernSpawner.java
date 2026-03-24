package com.pacificbytestudios.projectciviccore.village.core.spawner;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerEntryComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerRegistryComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceInteractibles;
import com.pacificbytestudios.projectciviccore.villagers.enums.WorkType;

import io.sentry.util.Pair;

public class TavernSpawner {
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
        StructureConfig.Tavern.PREFAB_JSON,
        position.clone(),
        structureRotation,
        true);
    if (trackedPlacement == null) {
      return false;
    }

    int blockRotation = RotationTuple.of(
        StructurePlacementUtils.addYaw(
            StructureConfig.Tavern.WORKPLACE_BLOCK_YAW_OFFSET,
            structureRotation),
        Rotation.None,
        Rotation.None).index();

    Vector3i workplaceBlockPosition = getWorkplaceBlockPosition(position, structureRotation);
    WorldChunk workplaceChunk = Utils.setBlockChunkAware(chunk,
        workplaceBlockPosition,
        StructureConfig.Tavern.WORKPLACE_BLOCK,
        blockRotation);
    Ref<ChunkStore> workplace = workplaceChunk.getBlockComponentEntity(
        workplaceBlockPosition.x,
        workplaceBlockPosition.y,
        workplaceBlockPosition.z);

    if (workplace == null || !workplace.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[Tavern - spawn] Workplace ref is invalid");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement,
          workplaceBlockPosition, null);
    }

    Store<ChunkStore> chunkStore = workplace.getStore();
    WorkplaceComponent workplaceComponent = chunkStore.getComponent(
        workplace,
        WorkplaceComponent.getComponentType());
    if (workplaceComponent == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[Tavern - spawn] Workplace component is missing");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement,
          workplaceBlockPosition, null);
    }

    WorkplaceInteractablesMapComponent interactables = chunkStore.getComponent(
        workplace,
        WorkplaceInteractablesMapComponent.getComponentType());
    if (interactables == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[Tavern - spawn] Interactables component is missing");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement,
          workplaceBlockPosition, null);
    }

    registerInteractables(position, interactables, structureRotation, 1);

    if (workplaceComponent.getUuid() == null) {
      workplaceComponent.setUuid(UUID.randomUUID());
    }
    workplaceComponent.setLevel((short) 1);
    workplaceComponent.setBenchPos(workplaceBlockPosition);
    workplaceComponent.setBenchAccessPos(getBenchAccessPosition(workplaceBlockPosition, structureRotation));
    workplaceComponent.setPrefabAnchor(position.clone());
    workplaceComponent.setWorkerType(new WorkType[] { WorkType.INN_KEEPER });
    workplaceComponent.setWorkplaceRef(new EntityRef(chunk.getWorld().getName(),
        workplaceBlockPosition));

    chunkStore.putComponent(
        workplace,
        TrackedPrefabPlacementComponent.getComponentType(),
        trackedPlacement);

    WorkplaceContainerRegistryComponent registry = spawnContainers(
        1, position, workplaceChunk, chunkStore, structureRotation);

    if (registry == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[Tavern - spawn] Could not spawn correctly the containers");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement,
          workplaceBlockPosition, null);
    }

    workplaceComponent.setStorage(registry);
    workplaceChunk.markNeedsSaving();
    return true;
  }

  private static WorkplaceContainerRegistryComponent spawnContainers(
      int level,
      Vector3i prefabAnchor,
      WorldChunk chunk,
      Store<ChunkStore> chunkStore,
      Rotation structureRotation) {
    WorkplaceContainerRegistryComponent registry = new WorkplaceContainerRegistryComponent();
    List<Vector3i> offsets = StructureConfig.Tavern.getStorageOffsetForLevel(level);
    WorldChunk usedChunk = chunk;
    Vector3i containerPosition;
    int rotation = RotationTuple.of(
        StructurePlacementUtils.addYaw(
            StructureConfig.Tavern.CONTAINER_BLOCK_YAW_OFFSET,
            structureRotation),
        Rotation.None,
        Rotation.None).index();

    for (Vector3i offset : offsets) {
      containerPosition = StructurePlacementUtils.withOffset(prefabAnchor, offset, structureRotation);

      usedChunk = Utils.setBlockChunkAware(
          usedChunk,
          containerPosition,
          StructureConfig.Tavern.SINGLE_STORAGE_BLOCK,
          rotation);

      Ref<ChunkStore> containerRef = usedChunk.getBlockComponentEntity(containerPosition.x,
          containerPosition.y, containerPosition.z);

      if (containerRef == null || !containerRef.isValid()) {
        ProjectCivicCore.LOGGER.atSevere().log("[Storage - spawn] Container ref is invalid");
        return null;
      }

      WorkplaceContainerEntryComponent container = chunkStore.getComponent(
          containerRef,
          WorkplaceContainerEntryComponent.getComponentType());

      if (container == null) {
        ProjectCivicCore.LOGGER.atSevere().log("[Storage - spawn] Container component is missing");
        return null;
      }

      container.setPosition(containerPosition);
      container.setAccessPosition(getContainerAccessPosition(containerPosition, structureRotation));
      container.setUuid(UUID.randomUUID());
      registry.registerContainer(container.getUuid(), container);
    }

    return registry;
  }

  private static void registerInteractables(
      Vector3i center,
      WorkplaceInteractablesMapComponent interactables,
      Rotation rotation,
      int level) {

    interactables.setNewInteractables(
        WorkplaceInteractibles.EATING_SPOT,
        StructureConfig.Tavern.getInteractables(center, rotation, level));
  }

  public static boolean despawn(WorldChunk chunk, Vector3i position) {
    Vector3i workplaceBlockPosition = position.clone();
    Pair<WorldChunk, Ref<ChunkStore>> result = Utils.getBlockComponentEntityChunkAware(
        chunk,
        workplaceBlockPosition);

    WorldChunk workplaceChunk = result.getFirst();
    Ref<ChunkStore> workplaceRef = result.getSecond();

    if (workplaceRef == null || !workplaceRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[Storage - despawn] Invalid workplaceRef");
      return false;
    }

    Store<ChunkStore> store = workplaceRef.getStore();

    WorkplaceComponent workplace = store.getComponent(workplaceRef,
        WorkplaceComponent.getComponentType());
    TrackedPrefabPlacementComponent trackedPlacement = store
        .getComponent(workplaceRef,
            TrackedPrefabPlacementComponent.getComponentType());

    if (workplace == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[Storage - despawn] Missing workplace component");
      return false;
    }

    Rotation prefabRotation = trackedPlacement != null ? trackedPlacement.getYaw() : Rotation.None;
    boolean despawn = false;
    if (trackedPlacement != null && trackedPlacement.hasRestoreSnapshot()) {
      despawn = PrefabManager.restoreTrackedPrefab(workplaceChunk.getWorld(),
          trackedPlacement);
    } else {
      Vector3i prefabAnchor = workplace.getPrefabAnchor();
      if (prefabAnchor == null && trackedPlacement != null) {
        prefabAnchor = trackedPlacement.getOrigin();
      }
      if (prefabAnchor == null) {
        prefabAnchor = getPrefabAnchorFromWorkplace(workplaceBlockPosition, prefabRotation);
      }

      despawn = PrefabManager.removePrefab(
          workplaceChunk.getWorld(),
          StructureConfig.Tavern.PREFAB_JSON,
          prefabAnchor,
          prefabRotation,
          true);
    }

    if (!despawn) {
      ProjectCivicCore.LOGGER.atSevere().log("[Storage - despawn] Could not despawn storage prefab");
      return false;
    }

    despawnContainers(chunk, workplace);
    Utils.breakBlockChunkAware(workplaceChunk, workplaceBlockPosition);
    return despawn;
  }

  private static void despawnContainers(WorldChunk chunk, WorkplaceComponent workplace) {
    Map<UUID, WorkplaceContainerEntryComponent> containers = workplace.getStorage().getContainers();

    Set<Entry<UUID, WorkplaceContainerEntryComponent>> entrySet = new HashSet<Entry<UUID, WorkplaceContainerEntryComponent>>(
        containers.entrySet());

    for (Map.Entry<UUID, WorkplaceContainerEntryComponent> container : entrySet) {
      workplace.getStorage().unregisterContainer(container.getKey());
      Utils.breakBlockChunkAware(chunk, container.getValue().getPosition());
    }
  }

  private static Vector3i getWorkplaceBlockPosition(Vector3i prefabAnchor, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        prefabAnchor,
        StructureConfig.Tavern.WORKPLACE_BLOCK_OFFSET,
        rotation);
  }

  private static Vector3i getBenchAccessPosition(Vector3i workplaceBlockPosition, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        workplaceBlockPosition,
        StructureConfig.Tavern.BENCH_ACCESS_OFFSET,
        rotation);
  }

  private static Vector3i getContainerAccessPosition(Vector3i containerPosition, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        containerPosition,
        StructureConfig.Tavern.CONTAINER_ACCESS_OFFSET,
        rotation);
  }

  private static Vector3i getPrefabAnchorFromWorkplace(Vector3i workplaceBlockPosition, Rotation rotation) {
    return StructurePlacementUtils.removeOffset(
        workplaceBlockPosition,
        StructureConfig.Tavern.WORKPLACE_BLOCK_OFFSET,
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
