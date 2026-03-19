package com.pacificbytestudios.projectciviccore.village.core.spawner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import com.pacificbytestudios.projectciviccore.codecs.Interactable;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
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
  private static final String PREFAB_JSON = "Structures/Workplace/Workplace_Tavern.prefab.json";
  private static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Tavern_Core");
  private static final BlockType SINGLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Small");

  public static boolean spawn(WorldChunk chunk, Vector3i position, VillageHeartComponent heart) {
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
        Rotation.Ninety,
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

    registerInteractables(position, workplaceComponent, interactables, 1);

    workplaceComponent.setLevel((short) 1);
    workplaceComponent.setBenchPos(workplaceBlockPosition);
    workplaceComponent.setBenchAccessPos(getBenchAccessPosition(workplaceBlockPosition));
    workplaceComponent.setWorkerType(new WorkType[] { WorkType.INN_KEEPER });
    workplaceComponent.setWorkplaceRef(new EntityRef(chunk.getWorld().getName(),
        workplaceBlockPosition));

    chunkStore.putComponent(
        workplace,
        TrackedPrefabPlacementComponent.getComponentType(),
        trackedPlacement);

    WorkplaceContainerRegistryComponent registry = spawnContainers(
        1, position, workplaceChunk, chunkStore, rotation);

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
      Vector3i center,
      WorldChunk chunk,
      Store<ChunkStore> chunkStore,
      int rotation) {
    WorkplaceContainerRegistryComponent registry = new WorkplaceContainerRegistryComponent();
    List<Vector3i> offsets = Config.getStorageOffsetForLevel(level);
    WorldChunk usedChunk = chunk;
    Vector3i containerPosition;

    for (Vector3i offset : offsets) {

      containerPosition = center.clone().add(offset);

      usedChunk = Utils.setBlockChunkAware(
          usedChunk,
          containerPosition,
          SINGLE_STORAGE_BLOCK,
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
      container.setAccessPosition(getContainerAccessPosition(containerPosition));
      container.setUuid(UUID.randomUUID());
      registry.registerContainer(container.getUuid(), container);
    }

    return registry;
  }

  private static void registerInteractables(
      Vector3i center,
      WorkplaceComponent workplace,
      WorkplaceInteractablesMapComponent interactables,
      int level) {

    interactables.setNewInteractables(
        WorkplaceInteractibles.EATING_SPOT,
        Config.getInteractables(center, level));
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

  private static Vector3i getWorkplaceBlockPosition(Vector3i prefabAnchor) {
    return prefabAnchor.clone()
        .add(Vector3i.NORTH.clone().scale(4))
        .add(Vector3i.WEST.clone().scale(4))
        .add(Vector3i.UP);
  }

  private static Vector3i getBenchAccessPosition(Vector3i workplaceBlockPosition) {
    return workplaceBlockPosition.clone().add(Vector3i.EAST);
  }

  private static Vector3i getContainerAccessPosition(Vector3i containerPosition) {
    return containerPosition.clone().add(Vector3i.EAST);
  }

  private static Vector3i getPrefabAnchorFromWorkplace(Vector3i workplaceBlockPosition) {
    return workplaceBlockPosition.clone().add(Vector3i.SOUTH.clone().scale(4)).add(Vector3i.DOWN);
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

  private class Config {
    private static final Map<Integer, List<Vector3i>> STORAGE_SPAWNING_OFFSETS;
    private static final Map<Integer, List<Interactable>> INTERACTABLES;

    static {
      STORAGE_SPAWNING_OFFSETS = new HashMap<>();
      INTERACTABLES = new HashMap<>();

      List<Vector3i> level1Containers = new ArrayList<>();
      level1Containers.add(new Vector3i(-4, 1, -2));
      STORAGE_SPAWNING_OFFSETS.put(1, level1Containers);

      List<Interactable> level1Interactables = new ArrayList<>();
      level1Interactables.add(new Interactable(new Vector3i(-2, 1, 1), new Vector3i(-2, 2, 2), true));
      level1Interactables.add(new Interactable(new Vector3i(-3, 1, 1), new Vector3i(-3, 2, 2), true));
      level1Interactables.add(new Interactable(new Vector3i(-2, 1, 4), new Vector3i(-2, 2, 3), true));
      level1Interactables.add(new Interactable(new Vector3i(-3, 1, 4), new Vector3i(-3, 2, 3), true));

      level1Interactables.add(new Interactable(new Vector3i(2, 1, 1), new Vector3i(2, 2, 2), true));
      level1Interactables.add(new Interactable(new Vector3i(3, 1, 1), new Vector3i(3, 2, 2), true));
      level1Interactables.add(new Interactable(new Vector3i(2, 1, 4), new Vector3i(2, 2, 3), true));
      level1Interactables.add(new Interactable(new Vector3i(3, 1, 4), new Vector3i(3, 2, 3), true));

      level1Interactables.add(new Interactable(new Vector3i(2, 1, -1), new Vector3i(2, 2, -2), true));
      level1Interactables.add(new Interactable(new Vector3i(3, 1, -1), new Vector3i(3, 2, -2), true));
      level1Interactables.add(new Interactable(new Vector3i(2, 1, -4), new Vector3i(2, 2, -3), true));
      level1Interactables.add(new Interactable(new Vector3i(3, 1, -4), new Vector3i(3, 2, -3), true));
      INTERACTABLES.put(1, level1Interactables);
    }

    public static List<Vector3i> getStorageOffsetForLevel(int level) {
      return STORAGE_SPAWNING_OFFSETS.get(level);
    }

    public static List<Interactable> getInteractables(Vector3i center, int level) {
      List<Interactable> template = INTERACTABLES.getOrDefault(level, new ArrayList<>());
      List<Interactable> result = new ArrayList<>(template.size());

      for (Interactable interactable : template) {
        if (interactable == null) {
          continue;
        }

        Interactable copy = interactable.clone();

        copy.setInteractablePosition(
            toAbsolutePositions(center, interactable.getInteractablePosition()));

        if (interactable.getDependsOn() != null) {
          copy.setDependsOn(
              toAbsolutePositions(center, interactable.getDependsOn()));
        }

        result.add(copy);
      }

      return result;
    }

    private static Vector3i toAbsolutePositions(Vector3i prefabAnchor, Vector3i offset) {
      return prefabAnchor.clone().add(offset);
    }
  }
}
