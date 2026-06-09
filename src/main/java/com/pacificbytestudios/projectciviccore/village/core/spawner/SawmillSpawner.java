package com.pacificbytestudios.projectciviccore.village.core.spawner;

import java.util.UUID;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.codecs.AreaFootprint;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.StructureLevelConfig;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.builder.BuildingSnapshotComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerEntryComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerRegistryComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequestInvalidation;
import com.pacificbytestudios.projectciviccore.village.enums.ConstructionMode;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageObjectiveType;
import com.pacificbytestudios.projectciviccore.villagers.enums.WorkType;

import io.sentry.util.Pair;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class SawmillSpawner {

  public static boolean beginNewConstruction(
      UUID structureId,
      WorldChunk chunk,
      Vec3i prefabAnchor,
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
      Vec3i prefabAnchor,
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
      Vec3i prefabAnchor,
      Rotation rotation,
      VillageHeartComponent heart,
      int targetLevel) {
    Logger.logInfo(
        "Registering new staged Sawmill construction of level: %d, with id: %s",
        targetLevel, structureId);

    WorldChunk chunkToUse = chunk;
    World world = chunkToUse.getWorld();
    short stage = (short) 0;
    StructureLevelConfig config = StructureSpawnConfig.get(heart, StructureType.SAWMILL);
    String prefab = StructureSpawnConfig.getPrefab(config, targetLevel, stage);

    Logger.logInfo("Spawning prefab at: %s", prefabAnchor);
    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        prefab,
        prefabAnchor.clone(),
        rotation,
        true);
    if (trackedPlacement == null) {
      Logger.logWarning("Could not spawn prefab");
      return false;
    }

    Vec3i constructionSignPosition = getConstructionSignBlockPosition(config, prefabAnchor, targetLevel, rotation);
    int constructionSignRotationIndex = getConstructionSignRotationIndex(config, targetLevel, rotation);
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
      Logger.logWarning("Could not fetch the construction sign entity");
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
    snapshotComponent.setStructureType(StructureType.SAWMILL);
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
      Vec3i existingCoreOrSitePosition,
      int targetLevel,
      ConstructionMode mode) {
    Pair<WorldChunk, Ref<ChunkStore>> result = Utils.getBlockComponentEntityChunkAware(
        chunk,
        existingCoreOrSitePosition);

    if (result == null || result.getSecond() == null ||
        !result.getSecond().isValid()) {
      Logger.logWarning("Could not fetch the existing sawmill entity");
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
      Logger.logWarning(
          "Could not fetch the existing sawmill component or terrain snapshot");
      return false;
    }

    Ref<ChunkStore> heartRef = workplace.getVillageHeartRef();
    VillageHeartComponent heart = heartRef.getStore().getComponent(
        heartRef,
        VillageHeartComponent.getComponentType());
    StructureLevelConfig config = StructureSpawnConfig.get(heart, StructureType.SAWMILL);

    UUID structureId = workplace.getUuid();
    int currentLevel = workplace.getLevel();
    Vec3i prefabAnchor = workplace.getPrefabAnchor();
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
        Logger.logWarning("Could not complete the sawmill upgrade, rolling back");
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
    String prefab = StructureSpawnConfig.getPrefab(config, targetLevel, stage);
    TrackedPrefabPlacementComponent stagedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        prefab,
        prefabAnchor.clone(),
        structureRotation,
        true);

    if (stagedPlacement == null) {
      Logger.logWarning("Could not spawn the first upgrade stage prefab");
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

    Vec3i constructionSignPosition = getConstructionSignBlockPosition(config, prefabAnchor, targetLevel,
        structureRotation);
    int constructionSignRotationIndex = getConstructionSignRotationIndex(config, targetLevel, structureRotation);

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
    snapshotComponent.setStructureType(StructureType.SAWMILL);
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
      Vec3i prefabAnchor,
      Rotation rotation,
      VillageHeartComponent heart) {

    WorldChunk chunkToUse = chunk;
    StructureLevelConfig config = StructureSpawnConfig.get(heart, StructureType.SAWMILL);
    Pair<WorldChunk, Ref<ChunkStore>> blockEntity = StructureSpawnConfig.findConstructionSign(
        chunkToUse,
        prefabAnchor,
        rotation,
        config,
        StructureType.SAWMILL);

    if (blockEntity == null) {
      Logger.logWarning("Could not fetch the construction sign entity");
      return false;
    }

    Ref<ChunkStore> constructionSignRef = blockEntity.getSecond();
    chunkToUse = blockEntity.getFirst();
    Store<ChunkStore> store = constructionSignRef.getStore();
    Vec3i constructionSignPosition = Utils.getWorldBlockContext(constructionSignRef, store).getPosition();

    BuildingSnapshotComponent snapshotComponent = store.getComponent(
        constructionSignRef,
        BuildingSnapshotComponent.getComponentType());

    if (!removePreviousStagePrefab(chunk, store, constructionSignRef, config,
        constructionSignPosition, snapshotComponent)) {
      throw new IllegalStateException("[advanceConstruction - sawmill] Could not despawn previous prefab");
    }

    snapshotComponent.setCurrentStage((short) (snapshotComponent.getCurrentStage() + 1));

    if (snapshotComponent.getCurrentStage() == VillageObjectiveType.getStages(StructureType.SAWMILL)) {
      VillageHeartComponent snapshotHeart = store.getComponent(
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
          snapshotHeart,
          snapshotComponent.getTargetLevel(),
          trackedPlacement);
    }

    return spawnMiddleStageBuilding(chunk, store, constructionSignRef, config,
        constructionSignPosition, snapshotComponent);
  }

  private static boolean removePreviousStagePrefab(
      WorldChunk chunk,
      Store<ChunkStore> store,
      Ref<ChunkStore> constructionSignRef,
      StructureLevelConfig config,
      Vec3i constructionSignPosition,
      BuildingSnapshotComponent snapshot) {
    TrackedPrefabPlacementComponent trackedPlacement = store
        .getComponent(constructionSignRef,
            TrackedPrefabPlacementComponent.getComponentType());

    Rotation prefabRotation = trackedPlacement != null ? trackedPlacement.getYaw() : Rotation.None;
    boolean despawn = false;
    String prefab = StructureSpawnConfig.getPrefab(config, snapshot.getTargetLevel(),
        snapshot.getCurrentStage());

    if (trackedPlacement != null && trackedPlacement.hasRestoreSnapshot()) {
      despawn = PrefabManager.restoreTrackedPrefab(chunk.getWorld(),
          trackedPlacement);
    } else {
      Vec3i prefabAnchor = snapshot.getPrefabAnchor();
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
      StructureLevelConfig config,
      Vec3i constructionSignPosition,
      BuildingSnapshotComponent snapshot) {

    String prefab = StructureSpawnConfig.getPrefab(config, snapshot.getTargetLevel(),
        snapshot.getCurrentStage());

    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        chunk.getWorld(),
        prefab,
        snapshot.getPrefabAnchor(),
        snapshot.getRotation(),
        true);

    return trackedPlacement != null;
  }

  private static boolean handleLastStageBuilding(
      UUID structureId,
      World world,
      WorldChunk chunk,
      Vec3i prefabAnchor,
      Rotation rotation,
      VillageHeartComponent heart,
      int targetLevel,
      TrackedPrefabPlacementComponent trackedPlacement) {
    StructureLevelConfig config = StructureSpawnConfig.get(heart, StructureType.SAWMILL);
    String prefab = StructureSpawnConfig.getCompletedPrefab(
        config,
        StructureType.SAWMILL,
        targetLevel);

    TrackedPrefabPlacementComponent snapshotToUse = trackedPlacement;

    TrackedPrefabPlacementComponent newSnapshot = PrefabManager.spawnTrackedPrefab(
        world,
        prefab,
        prefabAnchor,
        rotation,
        true);
    if (newSnapshot == null) {
      return false;
    }

    if (snapshotToUse == null) {
      snapshotToUse = newSnapshot;
    }

    int blockRotation = getWorkplaceBlockRotationIndex(config, targetLevel, rotation);

    Vec3i workplaceBlockPosition = getWorkplaceBlockPosition(config, prefabAnchor, targetLevel, rotation);
    WorldChunk workplaceChunk = Utils.setBlockChunkAware(
        chunk,
        workplaceBlockPosition,
        StructureConfig.Sawmill.WORKPLACE_BLOCK,
        blockRotation);
    Ref<ChunkStore> workplaceRef = workplaceChunk.getBlockComponentEntity(
        workplaceBlockPosition.x,
        workplaceBlockPosition.y,
        workplaceBlockPosition.z);

    if (workplaceRef == null || !workplaceRef.isValid()) {
      Logger.logWarning("Workplace ref is invalid");
      return rollbackFailedSpawn(world, workplaceChunk, snapshotToUse, workplaceBlockPosition, null);
    }

    Store<ChunkStore> chunkStore = workplaceRef.getStore();
    WorkplaceComponent workplaceComponent = chunkStore.getComponent(
        workplaceRef,
        WorkplaceComponent.getComponentType());

    if (workplaceComponent == null) {
      Logger.logWarning("Workplace component is missing");
      return rollbackFailedSpawn(world, workplaceChunk, snapshotToUse, workplaceBlockPosition, null);
    }

    workplaceComponent.setUuid(structureId);
    workplaceComponent.setLevel((short) targetLevel);
    workplaceComponent.setBenchPos(workplaceBlockPosition);
    workplaceComponent.setBenchAccessOffset(getBenchAccessOffset(config, targetLevel, rotation));
    workplaceComponent.setWorkerType(new WorkType[] { WorkType.WOOD_CUTTER });
    workplaceComponent.setWorkplaceRef(new EntityRef(chunk.getWorld().getName(), workplaceBlockPosition));
    workplaceComponent.setWorkingArea(new AreaFootprint(
        StructureConfig.Sawmill.getWorkingAreaOffset(targetLevel),
        prefabAnchor,
        rotation));
    chunkStore.putComponent(
        workplaceRef,
        TrackedPrefabPlacementComponent.getComponentType(),
        snapshotToUse);

    int containerRotation = getContainerRotationIndex(config, targetLevel, rotation);
    Vec3i containerPosition = getContainerPosition(config, prefabAnchor, targetLevel, rotation);
    WorldChunk containerChunk = Utils.setBlockChunkAware(
        workplaceChunk,
        containerPosition,
        StructureConfig.Sawmill.SINGLE_STORAGE_BLOCK,
        containerRotation);
    Ref<ChunkStore> containerRef = containerChunk.getBlockComponentEntity(containerPosition.x,
        containerPosition.y, containerPosition.z);

    if (containerRef == null || !containerRef.isValid()) {
      Logger.logWarning("Container ref is invalid");
      return rollbackFailedSpawn(world, workplaceChunk, snapshotToUse, workplaceBlockPosition, containerPosition);
    }

    WorkplaceContainerEntryComponent container = chunkStore.getComponent(
        containerRef,
        WorkplaceContainerEntryComponent.getComponentType());

    if (container == null) {
      Logger.logWarning("Container component is missing");
      return rollbackFailedSpawn(world, workplaceChunk, snapshotToUse, workplaceBlockPosition, containerPosition);
    }

    workplaceComponent.setPrefabAnchor(prefabAnchor);

    container.setPosition(containerPosition);
    container.setAccessOffset(getContainerAccessOffset(config, targetLevel, rotation));
    container.setUuid(UUID.randomUUID());

    WorkplaceContainerRegistryComponent storage = new WorkplaceContainerRegistryComponent();
    storage.registerContainer(container.getUuid(), container);
    workplaceComponent.setStorage(storage);

    removeConstructionSignIfPresent(config, chunk, prefabAnchor, targetLevel, rotation);
    workplaceChunk.markNeedsSaving();
    return true;
  }

  public static boolean despawnIncompleteBuilding(
      WorldChunk chunk,
      Vec3i prefabAnchor,
      int level,
      Rotation rotation,
      VillageHeartComponent heart) {
    if (chunk == null || prefabAnchor == null) {
      Logger.logWarning("Missing chunk or prefab anchor");
      return false;
    }

    Rotation structureRotation = StructurePlacementUtils.normalize(rotation);
    StructureLevelConfig config = StructureSpawnConfig.get(heart, StructureType.SAWMILL);
    WorldChunk chunkToUse = chunk;
    Vec3i constructionSignPosition = getConstructionSignBlockPosition(config, prefabAnchor, level, structureRotation);
    Pair<WorldChunk, Ref<ChunkStore>> blockEntity = Utils.getBlockComponentEntityChunkAware(
        chunkToUse,
        constructionSignPosition);

    if (blockEntity == null || blockEntity.getSecond() == null || !blockEntity.getSecond().isValid()) {
      Logger.logWarning("Could not fetch the construction sign entity");
      return false;
    }

    Ref<ChunkStore> constructionSignRef = blockEntity.getSecond();
    chunkToUse = blockEntity.getFirst();
    Store<ChunkStore> store = constructionSignRef.getStore();

    BuildingSnapshotComponent snapshotComponent = store.getComponent(
        constructionSignRef,
        BuildingSnapshotComponent.getComponentType());
    TrackedPrefabPlacementComponent trackedPlacement = store.getComponent(
        constructionSignRef,
        TrackedPrefabPlacementComponent.getComponentType());

    Rotation prefabRotation = trackedPlacement != null ? trackedPlacement.getYaw() : structureRotation;
    Vec3i targetAnchor = snapshotComponent != null && snapshotComponent.getPrefabAnchor() != null
        ? snapshotComponent.getPrefabAnchor()
        : prefabAnchor.clone();
    if (targetAnchor == null && trackedPlacement != null) {
      targetAnchor = trackedPlacement.getOrigin();
    }

    int targetLevel = snapshotComponent != null ? snapshotComponent.getTargetLevel() : level;
    short currentStage = snapshotComponent != null ? snapshotComponent.getCurrentStage() : (short) 0;

    boolean despawn = false;
    if (trackedPlacement != null && trackedPlacement.hasRestoreSnapshot()) {
      despawn = PrefabManager.restoreTrackedPrefab(chunkToUse.getWorld(), trackedPlacement);
    } else if (targetAnchor != null) {
      despawn = PrefabManager.removePrefab(
          chunkToUse.getWorld(),
          StructureSpawnConfig.getPrefab(config, targetLevel, currentStage),
          targetAnchor,
          prefabRotation,
          true);
    }

    if (!despawn) {
      Logger.logWarning("Could not despawn sawmill prefab");
      return false;
    }

    removeConstructionSignIfPresent(config, chunkToUse, targetAnchor, targetLevel, structureRotation);
    return true;
  }

  public static boolean despawn(WorldChunk chunk, Vec3i position) {
    Vec3i workplaceBlockPosition = position.clone();
    Pair<WorldChunk, Ref<ChunkStore>> result = Utils.getBlockComponentEntityChunkAware(
        chunk,
        workplaceBlockPosition);

    WorldChunk workplaceChunk = result.getFirst();
    Ref<ChunkStore> workplaceRef = result.getSecond();

    if (workplaceRef == null || !workplaceRef.isValid()) {
      Logger.logWarning("Invalid workplaceRef");
      return false;
    }

    Store<ChunkStore> store = workplaceRef.getStore();

    WorkplaceComponent component = store.getComponent(
        workplaceRef,
        WorkplaceComponent.getComponentType());
    TrackedPrefabPlacementComponent trackedPlacement = store.getComponent(
        workplaceRef,
        TrackedPrefabPlacementComponent.getComponentType());

    if (component == null) {
      Logger.logWarning("Missing workplace component");
      return false;
    }
    VillageHeartComponent heart = component.getVillageHeartRef().getStore().getComponent(
        component.getVillageHeartRef(),
        VillageHeartComponent.getComponentType());
    StructureLevelConfig config = StructureSpawnConfig.get(heart, StructureType.SAWMILL);

    Rotation prefabRotation = trackedPlacement != null ? trackedPlacement.getYaw() : Rotation.None;
    boolean despawn = false;
    if (trackedPlacement != null && trackedPlacement.hasRestoreSnapshot()) {
      despawn = PrefabManager.restoreTrackedPrefab(workplaceChunk.getWorld(), trackedPlacement);
    } else {
      Vec3i prefabAnchor = component.getPrefabAnchor() == null ? null : component.getPrefabAnchor();
      if (prefabAnchor == null && trackedPlacement != null) {
        prefabAnchor = trackedPlacement.getOrigin();
      }
      if (prefabAnchor == null) {
        prefabAnchor = getPrefabAnchorFromWorkplace(config, workplaceBlockPosition, component.getLevel(),
            prefabRotation);
      }

      despawn = PrefabManager.removePrefab(
          workplaceChunk.getWorld(),
          StructureSpawnConfig.getCompletedPrefab(config, StructureType.SAWMILL, component.getLevel()),
          prefabAnchor,
          prefabRotation,
          true);
    }

    if (!despawn) {
      Logger.logWarning("Could not despawn sawmill prefab");
      return false;
    }

    Vec3i prefabAnchor = component.getPrefabAnchor() == null ? null : component.getPrefabAnchor();
    if (prefabAnchor == null && trackedPlacement != null) {
      prefabAnchor = trackedPlacement.getOrigin();
    }
    if (prefabAnchor == null) {
      prefabAnchor = getPrefabAnchorFromWorkplace(config, workplaceBlockPosition, component.getLevel(),
          prefabRotation);
    }

    Vec3i containerPosition = getContainerPosition(config, prefabAnchor, component.getLevel(), prefabRotation);
    Utils.breakBlockChunkAware(workplaceChunk, workplaceBlockPosition);
    Utils.breakBlockChunkAware(workplaceChunk, containerPosition);

    return true;
  }

  private static void removeConstructionSignIfPresent(
      StructureLevelConfig config,
      WorldChunk chunk,
      Vec3i prefabAnchor,
      int level,
      Rotation rotation) {
    Vec3i constructionSignPosition = getConstructionSignBlockPosition(config, prefabAnchor, level, rotation);
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
      return;
    }

    signChunk = Utils.breakBlockChunkAware(signChunk, constructionSignPosition);
    signChunk.markNeedsSaving();
  }

  private static int getWorkplaceBlockRotationIndex(StructureLevelConfig config, int level, Rotation rotation) {
    return StructureSpawnConfig.blockRotationIndex(
        StructureSpawnConfig.getCoreBlock(config, level),
        rotation);
  }

  private static int getConstructionSignRotationIndex(StructureLevelConfig config, int level, Rotation rotation) {
    return StructureSpawnConfig.constructionSignRotationIndex(
        StructureSpawnConfig.getConstructionBlock(config, level),
        rotation);
  }

  private static int getContainerRotationIndex(StructureLevelConfig config, int level, Rotation rotation) {
    return StructureSpawnConfig.blockRotationIndex(
        StructureSpawnConfig.getFirstContainer(config, level),
        rotation);
  }

  private static Vec3i getWorkplaceBlockPosition(
      StructureLevelConfig config,
      Vec3i prefabAnchor,
      int level,
      Rotation rotation) {
    return StructureSpawnConfig.blockPosition(
        prefabAnchor,
        StructureSpawnConfig.getCoreBlock(config, level),
        rotation);
  }

  private static Vec3i getConstructionSignBlockPosition(
      StructureLevelConfig config,
      Vec3i prefabAnchor,
      int level,
      Rotation rotation) {
    return StructureSpawnConfig.blockPosition(
        prefabAnchor,
        StructureSpawnConfig.getConstructionBlock(config, level),
        rotation);
  }

  private static Vec3i getContainerPosition(
      StructureLevelConfig config,
      Vec3i prefabAnchor,
      int level,
      Rotation rotation) {
    return StructureSpawnConfig.blockPosition(
        prefabAnchor,
        StructureSpawnConfig.getFirstContainer(config, level),
        rotation);
  }

  private static Vec3f getBenchAccessOffset(
      StructureLevelConfig config,
      int level,
      Rotation rotation) {
    return StructureSpawnConfig.accessOffset(
        StructureSpawnConfig.getCoreBlock(config, level),
        rotation);
  }

  private static Vec3f getContainerAccessOffset(
      StructureLevelConfig config,
      int level,
      Rotation rotation) {
    return StructureSpawnConfig.accessOffset(
        StructureSpawnConfig.getFirstContainer(config, level),
        rotation);
  }

  private static Vec3i getPrefabAnchorFromWorkplace(
      StructureLevelConfig config,
      Vec3i workplaceBlockPosition,
      int level,
      Rotation rotation) {
    return StructureSpawnConfig.prefabAnchorFromBlock(
        workplaceBlockPosition,
        StructureSpawnConfig.getCoreBlock(config, level),
        rotation);
  }

  private static boolean rollbackFailedSpawn(
      World world,
      WorldChunk chunk,
      TrackedPrefabPlacementComponent trackedPlacement,
      Vec3i workplaceBlockPosition,
      Vec3i containerPosition) {
    Utils.breakBlockChunkAware(chunk, workplaceBlockPosition);
    if (containerPosition != null) {
      Utils.breakBlockChunkAware(chunk, containerPosition);
    }
    PrefabManager.restoreTrackedPrefab(world, trackedPlacement);
    return false;
  }
}
