package com.pacificbytestudios.projectciviccore.village.core.spawner;

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
import com.pacificbytestudios.projectciviccore.villagers.enums.WorkType;

import io.sentry.util.Pair;

public class TownHallSpawner {
  public static boolean spawn(WorldChunk chunk, Vector3i position, VillageHeartComponent village) {
    return spawn(chunk, position, Rotation.None, village);
  }

  public static boolean spawn(
      WorldChunk chunk,
      Vector3i position,
      Rotation rotation,
      VillageHeartComponent village) {
    Rotation structureRotation = StructurePlacementUtils.normalize(rotation);
    World world = chunk.getWorld();
    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        StructureConfig.TownHall.PREFAB_JSON,
        position.clone(),
        structureRotation,
        true);
    if (trackedPlacement == null) {
      return false;
    }

    int blockRotation = RotationTuple.of(
        StructurePlacementUtils.addYaw(
            StructureConfig.TownHall.WORKPLACE_BLOCK_YAW_OFFSET,
            structureRotation),
        Rotation.None,
        Rotation.None).index();

    Vector3i workplaceBlockPosition = getWorkplaceBlockPosition(position, structureRotation);
    WorldChunk workplaceChunk = Utils.setBlockChunkAware(chunk,
        workplaceBlockPosition,
        StructureConfig.TownHall.WORKPLACE_BLOCK,
        blockRotation);
    Ref<ChunkStore> workplace = workplaceChunk.getBlockComponentEntity(
        workplaceBlockPosition.x,
        workplaceBlockPosition.y,
        workplaceBlockPosition.z);

    if (workplace == null || !workplace.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[Town Hall - Spawn] Workplace ref is invalid");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement,
          workplaceBlockPosition, null);
    }

    Store<ChunkStore> chunkStore = workplace.getStore();
    WorkplaceComponent workplaceComponent = chunkStore.getComponent(
        workplace,
        WorkplaceComponent.getComponentType());
    if (workplaceComponent == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[Town Hall - Spawn] Workplace component is missing");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement,
          workplaceBlockPosition, null);
    }

    WorkplaceContainerRegistryComponent storage = buildWorkplaceStorageRegistry(
        chunk,
        chunkStore,
        village,
        structureRotation);
    if (storage == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[Town Hall - Spawn] Could not build workplace storage registry");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement,
          workplaceBlockPosition, null);
    }

    workplaceComponent.setStorage(storage);
    workplaceComponent.setLevel((short) 1);
    workplaceComponent.setBenchPos(workplaceBlockPosition);
    workplaceComponent.setBenchAccessPos(getBenchAccessPosition(workplaceBlockPosition, structureRotation));
    workplaceComponent.setPrefabAnchor(position.clone());
    workplaceComponent.setWorkerType(new WorkType[] { WorkType.MAYOR });
    workplaceComponent.setWorkplaceRef(new EntityRef(chunk.getWorld().getName(),
        workplaceBlockPosition));

    chunkStore.putComponent(
        workplace,
        TrackedPrefabPlacementComponent.getComponentType(),
        trackedPlacement);

    workplaceChunk.markNeedsSaving();
    return true;
  }

  private static WorkplaceContainerRegistryComponent buildWorkplaceStorageRegistry(
      WorldChunk chunk,
      Store<ChunkStore> chunkStore,
      VillageHeartComponent village,
      Rotation rotation) {
    Vector3i containerPosition = getContainerPosition(village);
    Ref<ChunkStore> containerRef = Utils.getBlockComponentEntityChunkAware(chunk, containerPosition).getSecond();
    if (containerRef == null || !containerRef.isValid()) {
      return null;
    }

    WorkplaceContainerEntryComponent container = chunkStore.getComponent(
        containerRef,
        WorkplaceContainerEntryComponent.getComponentType());

    if (container == null) {
      return null;
    }

    container.setPosition(containerPosition);
    container.setAccessPosition(StructurePlacementUtils.withOffset(
        containerPosition,
        StructureConfig.TownHall.CONTAINER_ACCESS_OFFSET,
        rotation));
    container.setUuid(UUID.randomUUID());

    WorkplaceContainerRegistryComponent storage = new WorkplaceContainerRegistryComponent();
    storage.registerContainer(container.getUuid(), container);
    return storage;
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
          StructureConfig.TownHall.PREFAB_JSON,
          prefabAnchor,
          prefabRotation,
          true);
    }

    if (!despawn) {
      ProjectCivicCore.LOGGER.atSevere().log("[Storage - despawn] Could not despawn storage prefab");
      return false;
    }

    Utils.breakBlockChunkAware(workplaceChunk, workplaceBlockPosition);
    return despawn;
  }

  private static Vector3i getWorkplaceBlockPosition(Vector3i prefabAnchor, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        prefabAnchor,
        StructureConfig.TownHall.WORKPLACE_BLOCK_OFFSET,
        rotation);
  }

  private static Vector3i getBenchAccessPosition(Vector3i workplaceBlockPosition, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        workplaceBlockPosition,
        StructureConfig.TownHall.BENCH_ACCESS_OFFSET,
        rotation);
  }

  private static Vector3i getPrefabAnchorFromWorkplace(Vector3i workplaceBlockPosition, Rotation rotation) {
    return StructurePlacementUtils.removeOffset(
        workplaceBlockPosition,
        StructureConfig.TownHall.WORKPLACE_BLOCK_OFFSET,
        rotation);
  }

  private static Vector3i getContainerPosition(VillageHeartComponent village) {
    return village.getBlockContext().getPosition().clone();
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
