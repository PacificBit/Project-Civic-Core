package com.pacificbytestudios.projectciviccore.village.core.spawner;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerEntryComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerRegistryComponent;
import com.pacificbytestudios.projectciviccore.villagers.enums.WorkType;

import io.sentry.util.Pair;

public class TownHallSpawner {
  private static final String PREFAB_JSON = "Structures/Workplace/Workplace_Town_Hall.prefab.json";
  private static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Town_Hall_Core");

  public static boolean spawn(WorldChunk chunk, Vector3i position, VillageHeartComponent village) {
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

    int rotation = RotationTuple.of(
        Rotation.OneEighty,
        Rotation.None,
        Rotation.None).index();

    Vector3i workplaceBlockPosition = getWorkplaceBlockPosition(position);
    WorldChunk workplaceChunk = Utils.setBlockChunkAware(chunk,
        workplaceBlockPosition, WORKPLACE_BLOCK, rotation);
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

    WorkplaceContainerRegistryComponent storage = buildWorkplaceStorageRegistry(world, chunk, chunkStore, village);
    if (storage == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[Town Hall - Spawn] Could not build workplace storage registry");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement,
          workplaceBlockPosition, null);
    }

    workplaceComponent.setStorage(storage);
    workplaceComponent.setLevel((short) 1);
    workplaceComponent.setBenchPos(workplaceBlockPosition);
    workplaceComponent.setBenchAccessPos(getBenchAccessPosition(workplaceBlockPosition));
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
      World world,
      WorldChunk chunk,
      Store<ChunkStore> chunkStore,
      VillageHeartComponent village) {
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
    container.setAccessPosition(containerPosition.clone().add(Vector3i.NORTH).add(Vector3i.DOWN));
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
        prefabAnchor = getPrefabAnchorFromWorkplace(workplaceBlockPosition);
      }

      despawn = PrefabManager.removePrefab(
          workplaceChunk.getWorld(),
          PREFAB_JSON,
          prefabAnchor,
          Rotation.None,
          true);
    }

    if (!despawn) {
      ProjectCivicCore.LOGGER.atSevere().log("[Storage - despawn] Could not despawn storage prefab");
      return false;
    }

    Utils.breakBlockChunkAware(workplaceChunk, workplaceBlockPosition);
    return despawn;
  }

  private static Vector3i getWorkplaceBlockPosition(Vector3i prefabAnchor) {
    return prefabAnchor.clone()
        .add(Vector3i.SOUTH.clone().scale(4))
        .add(Vector3i.WEST.clone().scale(4))
        .add(Vector3i.UP);
  }

  private static Vector3i getBenchAccessPosition(Vector3i workplaceBlockPosition) {
    return workplaceBlockPosition.clone().add(Vector3i.NORTH);
  }

  private static Vector3i getPrefabAnchorFromWorkplace(Vector3i workplaceBlockPosition) {
    return workplaceBlockPosition.clone().add(Vector3i.SOUTH.clone().scale(4)).add(Vector3i.DOWN);
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
