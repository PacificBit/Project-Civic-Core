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
import com.pacificbytestudios.projectciviccore.village.components.builder.BuildingSnapshotComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerEntryComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerRegistryComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequestInvalidation;
import com.pacificbytestudios.projectciviccore.village.enums.ConstructionMode;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageObjectiveType;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceInteractibles;
import com.pacificbytestudios.projectciviccore.villagers.enums.WorkType;

import io.sentry.util.Pair;

public class TavernSpawner {

  public static boolean beginNewConstruction(
      UUID structureId,
      WorldChunk chunk,
      Vector3i prefabAnchor,
      Rotation rotation,
      VillageHeartComponent heart,
      int targetLevel,
      ConstructionMode constructionMode) {
    Rotation structureRotation = StructurePlacementUtils.normalize(rotation);

    if (constructionMode == ConstructionMode.COMPLETED) {
      return handleNewCompletedConstruction(structureId, chunk, prefabAnchor, structureRotation, heart, targetLevel);
    }
    return handleNewStagedConstruction(structureId, chunk, prefabAnchor, structureRotation, heart, targetLevel);
  }

  private static boolean handleNewCompletedConstruction(
      UUID structureId,
      WorldChunk chunk,
      Vector3i prefabAnchor,
      Rotation rotation,
      VillageHeartComponent heart,
      int targetLevel) {
    return handleLastStageBuilding(
        structureId,
        chunk.getWorld(),
        chunk,
        prefabAnchor,
        rotation,
        heart,
        targetLevel,
        null);
  }

  private static boolean handleNewStagedConstruction(
      UUID structureId,
      WorldChunk chunk,
      Vector3i prefabAnchor,
      Rotation rotation,
      VillageHeartComponent heart,
      int targetLevel) {
    ProjectCivicCore.LOGGER.atInfo()
        .log(
            "[handleNewStagedConstruction - tavern] Registering new staged Tavern construction of level: " +
                targetLevel + ", with id: " + structureId);

    WorldChunk chunkToUse = chunk;
    World world = chunkToUse.getWorld();
    short stage = (short) 0;
    String prefab = StructureConfig.Tavern.getPrefab(targetLevel, stage);

    ProjectCivicCore.LOGGER.atInfo().log("[handleNewStagedConstruction - tavern] Spawning prefab at: " + prefabAnchor);
    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        prefab,
        prefabAnchor.clone(),
        rotation,
        true);
    if (trackedPlacement == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[handleNewStagedConstruction - tavern] Could not spawn prefab");
      return false;
    }

    Vector3i constructionSignPosition = getConstructionSignBlockPosition(prefabAnchor, targetLevel, rotation);
    int constructionSignRotationIndex = getConstructionSignRotationIndex(targetLevel, rotation);
    Utils.setBlockChunkAware(
        chunkToUse,
        constructionSignPosition,
        StructureConfig.CONSTRUCTION_SIGN,
        constructionSignRotationIndex);

    Pair<WorldChunk, Ref<ChunkStore>> result = Utils.getBlockComponentEntityChunkAware(
        chunkToUse,
        constructionSignPosition);

    if (result == null || result.getSecond() == null ||
        !result.getSecond().isValid()) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[handleNewStagedConstruction - tavern] Could not fetch the construction sign entity");
      return rollbackFailedSpawn(world, result.getFirst(), trackedPlacement,
          prefabAnchor, null);
    }
    chunkToUse = result.getFirst();
    Ref<ChunkStore> constructionSignRef = result.getSecond();
    Store<ChunkStore> store = constructionSignRef.getStore();

    BuildingSnapshotComponent snapshotComponent = store.getComponent(constructionSignRef,
        BuildingSnapshotComponent.getComponentType());

    snapshotComponent.setStructureId(structureId);
    snapshotComponent.setPrefabAnchor(prefabAnchor);
    snapshotComponent.setCurrentStage(stage);
    snapshotComponent.setRotation(rotation);
    snapshotComponent.setStructureType(StructureType.TAVERN);
    snapshotComponent.setVillageRef(heart.getEntityRef().clone());
    snapshotComponent.setTargetLevel((short) targetLevel);

    store.putComponent(
        constructionSignRef,
        TrackedPrefabPlacementComponent.getComponentType(),
        trackedPlacement);

    chunkToUse.markNeedsSaving();
    return true;
  }

  public static boolean beginUpgradeConstruction(
      WorldChunk chunk,
      Vector3i existingCoreOrSitePosition,
      int targetLevel,
      ConstructionMode mode) {
    Pair<WorldChunk, Ref<ChunkStore>> result = Utils.getBlockComponentEntityChunkAware(
        chunk,
        existingCoreOrSitePosition);

    if (result == null || result.getSecond() == null ||
        !result.getSecond().isValid()) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[beginUpgradeConstruction - tavern] Could not fetch the existing tavern entity");
      return false;
    }

    WorldChunk chunkToUse = result.getFirst();
    Ref<ChunkStore> workplaceRef = result.getSecond();
    Store<ChunkStore> store = workplaceRef.getStore();

    WorkplaceComponent workplace = store.getComponent(
        workplaceRef,
        WorkplaceComponent.getComponentType());
    TrackedPrefabPlacementComponent trackedPlacement = store.getComponent(
        workplaceRef,
        TrackedPrefabPlacementComponent.getComponentType());
    if (workplace == null || trackedPlacement == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[beginUpgradeConstruction - tavern] Could not fetch the existing tavern component or terrain snapshot");
      return false;
    }

    Ref<ChunkStore> heartRef = workplace.getVillageHeartRef();
    VillageHeartComponent heart = heartRef.getStore().getComponent(
        heartRef,
        VillageHeartComponent.getComponentType());

    UUID structureId = workplace.getUuid();
    int currentLevel = workplace.getLevel();
    Vector3i prefabAnchor = workplace.getPrefabAnchor().clone();
    Rotation structureRotation = StructurePlacementUtils.normalize(trackedPlacement.getYaw());
    EntityRef villageRefSnapshot = heart.getEntityRef().clone();
    TrackedPrefabPlacementComponent oldTrackedPlacement = trackedPlacement.clone();
    World world = chunkToUse.getWorld();

    if (mode == ConstructionMode.COMPLETED) {
      boolean upgraded = handleLastStageBuilding(
          structureId,
          world,
          chunkToUse,
          prefabAnchor,
          structureRotation,
          heart,
          targetLevel,
          oldTrackedPlacement);

      if (!upgraded) {
        ProjectCivicCore.LOGGER.atSevere()
            .log("[beginUpgradeConstruction - tavern] Could not complete the tavern upgrade, rolling back");
        handleLastStageBuilding(
            structureId,
            world,
            chunkToUse,
            prefabAnchor,
            structureRotation,
            heart,
            currentLevel,
            oldTrackedPlacement);
      }

      if (upgraded) {
        VillageStorageRequestInvalidation.invalidateRequesterWorkplace(heartRef, heart, structureId);
      }
      return upgraded;
    }

    short stage = (short) 0;
    String prefab = StructureConfig.Tavern.getPrefab(targetLevel, stage);
    TrackedPrefabPlacementComponent stagedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        prefab,
        prefabAnchor.clone(),
        structureRotation,
        true);

    if (stagedPlacement == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[beginUpgradeConstruction - tavern] Could not spawn the first upgrade stage prefab");
      handleLastStageBuilding(
          structureId,
          world,
          chunkToUse,
          prefabAnchor,
          structureRotation,
          heart,
          currentLevel,
          oldTrackedPlacement);
      return false;
    }

    Vector3i constructionSignPosition = getConstructionSignBlockPosition(prefabAnchor, targetLevel, structureRotation);
    int constructionSignRotationIndex = getConstructionSignRotationIndex(targetLevel, structureRotation);

    WorldChunk signChunk = Utils.setBlockChunkAware(
        chunkToUse,
        constructionSignPosition,
        StructureConfig.CONSTRUCTION_SIGN,
        constructionSignRotationIndex);
    Pair<WorldChunk, Ref<ChunkStore>> signResult = Utils.getBlockComponentEntityChunkAware(
        signChunk,
        constructionSignPosition);

    signChunk = signResult.getFirst();
    Ref<ChunkStore> constructionSignRef = signResult.getSecond();
    Store<ChunkStore> signStore = constructionSignRef.getStore();

    BuildingSnapshotComponent snapshotComponent = signStore.getComponent(
        constructionSignRef,
        BuildingSnapshotComponent.getComponentType());

    snapshotComponent.setStructureId(structureId);
    snapshotComponent.setPrefabAnchor(prefabAnchor);
    snapshotComponent.setCurrentStage(stage);
    snapshotComponent.setRotation(structureRotation);
    snapshotComponent.setStructureType(StructureType.TAVERN);
    snapshotComponent.setTargetLevel((short) targetLevel);
    snapshotComponent.setVillageRef(villageRefSnapshot);

    signStore.putComponent(
        constructionSignRef,
        TrackedPrefabPlacementComponent.getComponentType(),
        oldTrackedPlacement);

    signChunk.markNeedsSaving();
    VillageStorageRequestInvalidation.invalidateRequesterWorkplace(heartRef, heart, structureId);
    return true;
  }

  public static boolean advanceConstruction(
      WorldChunk chunk,
      Vector3i prefabAnchor,
      Rotation rotation) {

    WorldChunk chunkToUse = chunk;
    Vector3i constructionSignPosition = getConstructionSignBlockPosition(prefabAnchor, rotation);
    Pair<WorldChunk, Ref<ChunkStore>> blockEntity = Utils.getBlockComponentEntityChunkAware(
        chunkToUse,
        constructionSignPosition);

    if (blockEntity == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[advanceConstruction - tavern] Could not fetch the construction sign entity");
    }

    Ref<ChunkStore> constructionSignRef = blockEntity.getSecond();
    chunkToUse = blockEntity.getFirst();
    Store<ChunkStore> store = constructionSignRef.getStore();

    BuildingSnapshotComponent snapshotComponent = store.getComponent(
        constructionSignRef,
        BuildingSnapshotComponent.getComponentType());

    if (!removePreviousStagePrefab(chunk, store, constructionSignRef,
        constructionSignPosition, snapshotComponent)) {
      throw new IllegalStateException("[advanceConstruction - tavern] Could not despawn previous prefab");
    }

    snapshotComponent.setCurrentStage((short) (snapshotComponent.getCurrentStage() + 1));

    if (snapshotComponent.getCurrentStage() == VillageObjectiveType.getStages(StructureType.TAVERN)) {
      VillageHeartComponent heart = store.getComponent(
          snapshotComponent.getVillageRef().getRef(),
          VillageHeartComponent.getComponentType());

      TrackedPrefabPlacementComponent trackedPlacement = store
          .getComponent(constructionSignRef,
              TrackedPrefabPlacementComponent.getComponentType());

      return handleLastStageBuilding(
          snapshotComponent.getStructureId(),
          chunkToUse.getWorld(),
          chunkToUse,
          snapshotComponent.getPrefabAnchor(),
          snapshotComponent.getRotation(),
          heart,
          snapshotComponent.getTargetLevel(),
          trackedPlacement);
    }

    return spawnMiddleStageBuilding(chunk, store, constructionSignRef,
        constructionSignPosition, snapshotComponent);
  }

  private static boolean removePreviousStagePrefab(
      WorldChunk chunk,
      Store<ChunkStore> store,
      Ref<ChunkStore> constructionSignRef,
      Vector3i constructionSignPosition,
      BuildingSnapshotComponent snapshot) {
    TrackedPrefabPlacementComponent trackedPlacement = store
        .getComponent(constructionSignRef,
            TrackedPrefabPlacementComponent.getComponentType());

    Rotation prefabRotation = trackedPlacement != null ? trackedPlacement.getYaw() : Rotation.None;
    boolean despawn = false;
    String prefab = StructureConfig.Tavern.getPrefab(snapshot.getTargetLevel(),
        snapshot.getCurrentStage());

    if (trackedPlacement != null && trackedPlacement.hasRestoreSnapshot()) {
      despawn = PrefabManager.restoreTrackedPrefab(chunk.getWorld(),
          trackedPlacement);
    } else {
      Vector3i prefabAnchor = snapshot.getPrefabAnchor();
      if (prefabAnchor == null && trackedPlacement != null) {
        prefabAnchor = trackedPlacement.getOrigin();
      }

      despawn = PrefabManager.removePrefab(
          chunk.getWorld(),
          prefab,
          prefabAnchor,
          prefabRotation,
          true);
    }

    return despawn;
  }

  private static boolean spawnMiddleStageBuilding(
      WorldChunk chunk,
      Store<ChunkStore> store,
      Ref<ChunkStore> constructionSignRef,
      Vector3i constructionSignPosition,
      BuildingSnapshotComponent snapshot) {

    String prefab = StructureConfig.Tavern.getPrefab(snapshot.getTargetLevel(),
        snapshot.getCurrentStage());

    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        chunk.getWorld(),
        prefab,
        snapshot.getPrefabAnchor().clone(),
        snapshot.getRotation(),
        true);

    return trackedPlacement != null;
  }

  private static boolean handleLastStageBuilding(
      UUID structureId,
      World world,
      WorldChunk chunk,
      Vector3i prefabAnchor,
      Rotation rotation,
      VillageHeartComponent heart,
      int targetLevel,
      TrackedPrefabPlacementComponent trackedPlacement) {
    String prefab = StructureConfig.Tavern.getPrefab(
        targetLevel,
        VillageObjectiveType.getStages(StructureType.TAVERN));

    TrackedPrefabPlacementComponent snapshotToUse = trackedPlacement;

    TrackedPrefabPlacementComponent newSnapshot = PrefabManager.spawnTrackedPrefab(
        world,
        prefab,
        prefabAnchor.clone(),
        rotation,
        true);
    if (newSnapshot == null) {
      return false;
    }

    if (snapshotToUse == null) {
      snapshotToUse = newSnapshot;
    }

    int blockRotation = RotationTuple.of(
        StructurePlacementUtils.addYaw(
            StructureConfig.Tavern.getWorkplaceSpawningOffset(targetLevel).rotation(),
            rotation),
        Rotation.None,
        Rotation.None).index();

    Vector3i workplaceBlockPosition = getWorkplaceBlockPosition(prefabAnchor, targetLevel, rotation);
    WorldChunk workplaceChunk = Utils.setBlockChunkAware(chunk,
        workplaceBlockPosition,
        StructureConfig.Tavern.WORKPLACE_BLOCK,
        blockRotation);
    Ref<ChunkStore> workplaceRef = workplaceChunk.getBlockComponentEntity(
        workplaceBlockPosition.x,
        workplaceBlockPosition.y,
        workplaceBlockPosition.z);

    if (workplaceRef == null || !workplaceRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[Tavern - spawn] Workplace ref is invalid");
      return rollbackFailedSpawn(world, workplaceChunk, snapshotToUse,
          workplaceBlockPosition, null);
    }

    Store<ChunkStore> chunkStore = workplaceRef.getStore();
    WorkplaceComponent workplaceComponent = chunkStore.getComponent(
        workplaceRef,
        WorkplaceComponent.getComponentType());
    if (workplaceComponent == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[Tavern - spawn] Workplace component is missing");
      return rollbackFailedSpawn(world, workplaceChunk, snapshotToUse,
          workplaceBlockPosition, null);
    }

    WorkplaceInteractablesMapComponent interactables = chunkStore.getComponent(
        workplaceRef,
        WorkplaceInteractablesMapComponent.getComponentType());
    if (interactables == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[Tavern - spawn] Interactables component is missing");
      return rollbackFailedSpawn(world, workplaceChunk, snapshotToUse,
          workplaceBlockPosition, null);
    }

    registerInteractables(prefabAnchor, interactables, rotation, targetLevel);

    workplaceComponent.setUuid(structureId);
    workplaceComponent.setLevel((short) targetLevel);
    workplaceComponent.setBenchPos(workplaceBlockPosition);
    workplaceComponent.setBenchAccessPos(getBenchAccessPosition(workplaceBlockPosition, targetLevel, rotation));
    workplaceComponent.setPrefabAnchor(prefabAnchor.clone());
    workplaceComponent.setWorkerType(new WorkType[] { WorkType.INN_KEEPER });
    workplaceComponent.setWorkplaceRef(new EntityRef(chunk.getWorld().getName(),
        workplaceBlockPosition));

    chunkStore.putComponent(
        workplaceRef,
        TrackedPrefabPlacementComponent.getComponentType(),
        snapshotToUse);

    WorkplaceContainerRegistryComponent registry = spawnContainers(
        targetLevel, prefabAnchor, workplaceChunk, chunkStore, rotation);

    if (registry == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[Tavern - spawn] Could not spawn correctly the containers");
      return rollbackFailedSpawn(world, workplaceChunk, snapshotToUse,
          workplaceBlockPosition, null);
    }

    workplaceComponent.setStorage(registry);
    removeConstructionSignIfPresent(chunk, prefabAnchor, targetLevel, rotation);
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
    List<StructureConfig.ContainerTransform> layouts = StructureConfig.Tavern.getLevelLayout(level).containers();
    WorldChunk usedChunk = chunk;
    Vector3i containerPosition;

    for (StructureConfig.ContainerTransform containerLayout : layouts) {
      int rotation = RotationTuple.of(
          StructurePlacementUtils.addYaw(
              containerLayout.rotation(),
              structureRotation),
          Rotation.None,
          Rotation.None).index();
      containerPosition = StructurePlacementUtils.withOffset(prefabAnchor, containerLayout.position(),
          structureRotation);

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
      container.setAccessPosition(getContainerAccessPosition(containerPosition, containerLayout, structureRotation));
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
        prefabAnchor = getPrefabAnchorFromWorkplace(workplaceBlockPosition, prefabRotation, workplace.getLevel());
      }

      despawn = PrefabManager.removePrefab(
          workplaceChunk.getWorld(),
          StructureConfig.Tavern.getCompletedPrefab(workplace.getLevel()),
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

  private static void removeConstructionSignIfPresent(
      WorldChunk chunk,
      Vector3i prefabAnchor,
      int level,
      Rotation rotation) {
    Vector3i constructionSignPosition = getConstructionSignBlockPosition(prefabAnchor, level, rotation);
    Pair<WorldChunk, Ref<ChunkStore>> signResult = Utils.getBlockComponentEntityChunkAware(
        chunk,
        constructionSignPosition);
    if (signResult == null) {
      return;
    }

    WorldChunk signChunk = signResult.getFirst();
    Ref<ChunkStore> signRef = signResult.getSecond();
    if (signChunk == null || signRef == null || !signRef.isValid()) {
      return;
    }

    Store<ChunkStore> signStore = signRef.getStore();
    BuildingSnapshotComponent snapshotComponent = signStore.getComponent(
        signRef,
        BuildingSnapshotComponent.getComponentType());
    if (snapshotComponent == null) {
      clearArtefactBlocks(chunk, constructionSignPosition);
      return;
    }

    signChunk = Utils.breakBlockChunkAware(signChunk, constructionSignPosition);
    signChunk.markNeedsSaving();
  }

  private static void clearArtefactBlocks(WorldChunk chunk, Vector3i constructionPos) {
    Utils.breakBlockChunkAware(chunk, constructionPos);
    Utils.breakBlockChunkAware(chunk, constructionPos.clone().add(Vector3i.UP));
  }

  private static int getConstructionSignRotationIndex(int level, Rotation rotation) {
    Rotation signRotation = StructurePlacementUtils.addYaw(
        StructureConfig.Tavern.getLevelLayout(level).constructionSign().rotation(),
        rotation);
    return RotationTuple.of(signRotation, Rotation.None, Rotation.None).index();
  }

  private static Vector3i getWorkplaceBlockPosition(Vector3i prefabAnchor, int level, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        prefabAnchor,
        StructureConfig.Tavern.getLevelLayout(level).workplace().position(),
        rotation);
  }

  private static Vector3i getConstructionSignBlockPosition(Vector3i prefabAnchor, int level, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        prefabAnchor,
        StructureConfig.Tavern.getLevelLayout(level).constructionSign().position(),
        rotation);
  }

  private static Vector3i getConstructionSignBlockPosition(Vector3i prefabAnchor, Rotation rotation) {
    return getConstructionSignBlockPosition(prefabAnchor, 1, rotation);
  }

  private static Vector3i getBenchAccessPosition(Vector3i workplaceBlockPosition, int level, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        workplaceBlockPosition,
        StructureConfig.Tavern.getLevelLayout(level).benchAccessOffset(),
        rotation);
  }

  private static Vector3i getContainerAccessPosition(
      Vector3i containerPosition,
      StructureConfig.ContainerTransform containerLayout,
      Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        containerPosition,
        containerLayout.accessOffset(),
        rotation);
  }

  private static Vector3i getPrefabAnchorFromWorkplace(
      Vector3i workplaceBlockPosition,
      Rotation rotation,
      int level) {
    return StructurePlacementUtils.removeOffset(
        workplaceBlockPosition,
        StructureConfig.Tavern.getWorkplaceSpawningOffset(level).position(),
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
