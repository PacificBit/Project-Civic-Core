package com.pacificbytestudios.projectciviccore.village.core.spawner;

import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Stream;

import org.joml.Vector3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
import com.pacificbytestudios.projectciviccore.village.components.builder.BuildingSnapshotComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerEntryComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerRegistryComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.Mineshaft;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineshaftGallery;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineshaftLevel;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequestInvalidation;
import com.pacificbytestudios.projectciviccore.village.enums.ConstructionMode;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageObjectiveType;
import com.pacificbytestudios.projectciviccore.villagers.enums.WorkType;

import io.sentry.util.Pair;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class MineSpawner {

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
    Logger.logInfo(
            "Registering new staged Mine construction of level: " +
                targetLevel + ", with id: " + structureId);

    WorldChunk chunkToUse = chunk;
    World world = chunkToUse.getWorld();
    short stage = (short) 0;
    String prefab = StructureConfig.Mine.getPrefab(targetLevel, stage);

    Logger.logInfo("Spawning prefab at: " + prefabAnchor);
    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        prefab,
        VectorUtil.clone(prefabAnchor),
        rotation,
        true);
    if (trackedPlacement == null) {
      Logger.logWarning("Could not spawn prefab");
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
    snapshotComponent.setStructureType(StructureType.MINE);
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
      Logger.logWarning("Could not fetch the existing mine entity");
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
      Logger.logWarning("Could not fetch the existing mine component or terrain snapshot");
      return false;
    }

    Ref<ChunkStore> heartRef = workplace.getVillageHeartRef();
    VillageHeartComponent heart = heartRef.getStore().getComponent(
        heartRef,
        VillageHeartComponent.getComponentType());

    UUID structureId = workplace.getUuid();
    int currentLevel = workplace.getLevel();
    Vector3i prefabAnchor = VectorUtil.clone(workplace.getPrefabAnchor());
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
        Logger.logWarning("Could not complete the mine upgrade, rolling back");
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
    String prefab = StructureConfig.Mine.getPrefab(targetLevel, stage);
    TrackedPrefabPlacementComponent stagedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        prefab,
        VectorUtil.clone(prefabAnchor),
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
    snapshotComponent.setStructureType(StructureType.MINE);
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
      Logger.logWarning("Could not fetch the construction sign entity");
    }

    Ref<ChunkStore> constructionSignRef = blockEntity.getSecond();
    chunkToUse = blockEntity.getFirst();
    Store<ChunkStore> store = constructionSignRef.getStore();

    BuildingSnapshotComponent snapshotComponent = store.getComponent(
        constructionSignRef,
        BuildingSnapshotComponent.getComponentType());

    if (!removePreviousStagePrefab(chunk, store, constructionSignRef,
        constructionSignPosition, snapshotComponent)) {
      throw new IllegalStateException("[advanceConstruction - mine] Could not despawn previous prefab");
    }

    snapshotComponent.setCurrentStage((short) (snapshotComponent.getCurrentStage() + 1));

    if (snapshotComponent.getCurrentStage() == VillageObjectiveType.getStages(StructureType.MINE)) {
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
    String prefab = StructureConfig.Mine.getPrefab(snapshot.getTargetLevel(),
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

    String prefab = StructureConfig.Mine.getPrefab(snapshot.getTargetLevel(),
        snapshot.getCurrentStage());

    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        chunk.getWorld(),
        prefab,
        VectorUtil.clone(snapshot.getPrefabAnchor()),
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
    String prefab = StructureConfig.Mine.getPrefab(
        targetLevel,
        VillageObjectiveType.getStages(StructureType.MINE));

    TrackedPrefabPlacementComponent snapshotToUse = trackedPlacement;

    TrackedPrefabPlacementComponent newSnapshot = PrefabManager.spawnTrackedPrefab(
        world,
        prefab,
        VectorUtil.clone(prefabAnchor),
        rotation,
        true);
    if (newSnapshot == null) {
      return false;
    }

    if (snapshotToUse == null) {
      snapshotToUse = newSnapshot;
    }

    int blockRotation = getWorkplaceBlockRotationIndex(targetLevel, rotation);

    Vector3i workplaceBlockPosition = getWorkplaceBlockPosition(prefabAnchor, targetLevel, rotation);
    WorldChunk workplaceChunk = Utils.setBlockChunkAware(
        chunk,
        workplaceBlockPosition,
        StructureConfig.Mine.WORKPLACE_BLOCK,
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

    MineComponent mineComponent = chunkStore.getComponent(workplaceRef, MineComponent.getComponentType());

    if (workplaceComponent == null || mineComponent == null) {
      Logger.logWarning("Workplace component is missing");
      return rollbackFailedSpawn(world, workplaceChunk, snapshotToUse, workplaceBlockPosition, null);
    }

    Mineshaft mineshaft = new Mineshaft(
        StructureConfig.Mine.getMineshaftGalleriesOffset(),
        VectorUtil.clone(prefabAnchor),
        rotation);
    mineshaft.createNewMineshaftLevel();
    mineComponent.setMineshaft(mineshaft);
    Stream.of(mineshaft.getMineshaftLevels())
        .flatMap(level -> level.getGalleries().keySet().stream())
        .forEach(mineshaft::markGalleryAsCompleted);

    workplaceComponent.setUuid(structureId);
    workplaceComponent.setLevel((short) targetLevel);
    workplaceComponent.setBenchPos(workplaceBlockPosition);
    workplaceComponent.setBenchAccessPos(getBenchAccessPosition(workplaceBlockPosition, targetLevel, rotation));
    workplaceComponent.setWorkerType(new WorkType[] { WorkType.MINER });
    workplaceComponent.setWorkplaceRef(new EntityRef(chunk.getWorld().getName(), workplaceBlockPosition));
    chunkStore.putComponent(
        workplaceRef,
        TrackedPrefabPlacementComponent.getComponentType(),
        snapshotToUse);

    workplaceComponent.setPrefabAnchor(VectorUtil.clone(prefabAnchor));

    int containerRotation = getContainerRotationIndex(targetLevel, rotation);
    Vector3i containerPosition = getContainerPosition(prefabAnchor, targetLevel, rotation);
    WorldChunk containerChunk = Utils.setBlockChunkAware(
        workplaceChunk,
        containerPosition,
        StructureConfig.Mine.SINGLE_STORAGE_BLOCK,
        containerRotation);
    Ref<ChunkStore> containerRef = containerChunk.getBlockComponentEntity(
        containerPosition.x,
        containerPosition.y,
        containerPosition.z);

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

    container.setPosition(containerPosition);
    container.setAccessPosition(getContainerAccessPosition(containerPosition, targetLevel, rotation));
    container.setUuid(UUID.randomUUID());

    WorkplaceContainerRegistryComponent storage = new WorkplaceContainerRegistryComponent();
    storage.registerContainer(container.getUuid(), container);
    workplaceComponent.setStorage(storage);

    removeConstructionSignIfPresent(chunk, prefabAnchor, targetLevel, rotation);
    workplaceChunk.markNeedsSaving();
    return true;
  }

  public static boolean despawnIncompleteBuilding(
      WorldChunk chunk,
      Vector3i prefabAnchor,
      int level,
      Rotation rotation) {
    if (chunk == null || prefabAnchor == null) {
      Logger.logWarning("Missing chunk or prefab anchor");
      return false;
    }

    Rotation structureRotation = StructurePlacementUtils.normalize(rotation);
    WorldChunk chunkToUse = chunk;
    Vector3i constructionSignPosition = getConstructionSignBlockPosition(prefabAnchor, level, structureRotation);
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
    Vector3i targetAnchor = snapshotComponent != null && snapshotComponent.getPrefabAnchor() != null
        ? snapshotComponent.getPrefabAnchor()
        : VectorUtil.clone(prefabAnchor);
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
          StructureConfig.Mine.getPrefab(targetLevel, currentStage),
          targetAnchor,
          prefabRotation,
          true);
    }

    if (!despawn) {
      Logger.logWarning("Could not despawn mine prefab");
      return false;
    }

    removeConstructionSignIfPresent(chunkToUse, targetAnchor, targetLevel, structureRotation);
    return true;
  }

  public static boolean despawn(WorldChunk chunk, Vector3i position) {
    Vector3i workplaceBlockPosition = VectorUtil.clone(position);
    Pair<WorldChunk, Ref<ChunkStore>> result = Utils.getBlockComponentEntityChunkAware(
        chunk,
        workplaceBlockPosition);

    WorldChunk workplaceChunk = result.getFirst();
    Ref<ChunkStore> workplaceRef = result.getSecond();

    if (workplaceRef == null || !workplaceRef.isValid()) {
      for (int level = 1; level <= 3 && (workplaceRef == null || !workplaceRef.isValid()); level++) {
        workplaceBlockPosition = getWorkplaceBlockPosition(position, level, Rotation.None);
        result = Utils.getBlockComponentEntityChunkAware(chunk, workplaceBlockPosition);
        workplaceChunk = result.getFirst();
        workplaceRef = result.getSecond();
      }
    }

    if (workplaceRef == null || !workplaceRef.isValid()) {
      Logger.logWarning("Invalid workplaceRef");
      return false;
    }

    Store<ChunkStore> store = workplaceRef.getStore();

    WorkplaceComponent component = store.getComponent(
        workplaceRef,
        WorkplaceComponent.getComponentType());
    MineComponent mineComponent = store.getComponent(
        workplaceRef,
        MineComponent.getComponentType());
    TrackedPrefabPlacementComponent trackedPlacement = store.getComponent(
        workplaceRef,
        TrackedPrefabPlacementComponent.getComponentType());

    if (component == null || mineComponent == null) {
      Logger.logWarning("Missing workplace component");
      return false;
    }

    despawnMineshaft(workplaceChunk.getWorld(), mineComponent.getMineshaft());

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
        prefabAnchor = getPrefabAnchorFromWorkplace(workplaceBlockPosition, component.getLevel(), prefabRotation);
      }

      despawn = PrefabManager.removePrefab(
          workplaceChunk.getWorld(),
          StructureConfig.Mine.getCompletedPrefab(component.getLevel()),
          prefabAnchor,
          prefabRotation,
          true);
    }

    if (!despawn) {
      Logger.logWarning("Could not despawn mine prefab");
      return false;
    }

    Vector3i prefabAnchor = component.getPrefabAnchor();
    if (prefabAnchor == null && trackedPlacement != null) {
      prefabAnchor = trackedPlacement.getOrigin();
    }
    if (prefabAnchor == null) {
      prefabAnchor = getPrefabAnchorFromWorkplace(workplaceBlockPosition, component.getLevel(), prefabRotation);
    }

    Vector3i containerPosition = getContainerPosition(prefabAnchor, component.getLevel(), prefabRotation);
    Utils.breakBlockChunkAware(workplaceChunk, workplaceBlockPosition);
    Utils.breakBlockChunkAware(workplaceChunk, containerPosition);

    Utils.markNeedSaving(workplaceChunk.getWorld(), prefabAnchor);
    return true;
  }

  private static void despawnMineshaft(World world, Mineshaft mineshaft) {
    if (mineshaft == null) {
      return;
    }

    for (MineshaftLevel level : mineshaft.getMineshaftLevels()) {
      despawnGallery(world, level);
      TrackedPrefabPlacementComponent prefab = level.getTrackedPrefab();

      if (prefab == null) {
        continue;
      }

      if (prefab.hasRestoreSnapshot()) {
        PrefabManager.restoreTrackedPrefab(world, prefab);
        continue;
      }

      String prefabPath = prefab.getPrefabPath() != null
          ? prefab.getPrefabPath()
          : StructureConfig.Mine.MineshaftLevelConf.PREFAB_JSON;
      Vector3i prefabAnchor = level.getAnchorPoint() != null
          ? level.getAnchorPoint()
          : prefab.getOrigin();

      PrefabManager.removePrefab(
          world,
          prefabPath,
          prefabAnchor,
          mineshaft.getRotation(),
          true);
    }
  }

  private static void despawnGallery(World world, MineshaftLevel mineshaftLevel) {
    for (Entry<UUID, MineshaftGallery> gallery : mineshaftLevel.getGalleries().entrySet()) {
      for (TrackedPrefabPlacementComponent prefab : gallery.getValue().getRestoreSnapshot()) {
        if (prefab == null) {
          continue;
        }

        if (prefab.hasRestoreSnapshot()) {
          PrefabManager.restoreTrackedPrefab(world, prefab);
        }
      }
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
      return;
    }

    signChunk = Utils.breakBlockChunkAware(signChunk, constructionSignPosition);
    signChunk.markNeedsSaving();
  }

  private static int getWorkplaceBlockRotationIndex(int level, Rotation rotation) {
    Rotation workplaceRotation = StructurePlacementUtils.addYaw(
        StructureConfig.Mine.getLevelLayout(level).workplace().rotation(),
        rotation);
    return RotationTuple.of(workplaceRotation, Rotation.None, Rotation.None).index();
  }

  private static int getConstructionSignRotationIndex(int level, Rotation rotation) {
    Rotation signRotation = StructurePlacementUtils.addYaw(
        StructureConfig.Mine.getLevelLayout(level).constructionSign().rotation(),
        rotation);
    return RotationTuple.of(signRotation, Rotation.None, Rotation.None).index();
  }

  private static int getContainerRotationIndex(int level, Rotation rotation) {
    Rotation containerRotation = StructurePlacementUtils.addYaw(
        StructureConfig.Mine.getLevelLayout(level).container().rotation(),
        rotation);
    return RotationTuple.of(containerRotation, Rotation.None, Rotation.None).index();
  }

  private static Vector3i getWorkplaceBlockPosition(Vector3i prefabAnchor, int level, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        prefabAnchor,
        StructureConfig.Mine.getLevelLayout(level).workplace().position(),
        rotation);
  }

  private static Vector3i getConstructionSignBlockPosition(Vector3i prefabAnchor, int level, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        prefabAnchor,
        StructureConfig.Mine.getLevelLayout(level).constructionSign().position(),
        rotation);
  }

  private static Vector3i getConstructionSignBlockPosition(Vector3i prefabAnchor, Rotation rotation) {
    return getConstructionSignBlockPosition(prefabAnchor, 1, rotation);
  }

  private static Vector3i getContainerPosition(Vector3i prefabAnchor, int level, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        prefabAnchor,
        StructureConfig.Mine.getLevelLayout(level).container().position(),
        rotation);
  }

  private static Vector3i getBenchAccessPosition(Vector3i workplaceBlockPosition, int level, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        workplaceBlockPosition,
        StructureConfig.Mine.getLevelLayout(level).benchAccessOffset(),
        rotation);
  }

  private static Vector3i getContainerAccessPosition(Vector3i containerPosition, int level, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        containerPosition,
        StructureConfig.Mine.getLevelLayout(level).container().accessOffset(),
        rotation);
  }

  private static Vector3i getPrefabAnchorFromWorkplace(Vector3i workplaceBlockPosition, int level, Rotation rotation) {
    return StructurePlacementUtils.removeOffset(
        workplaceBlockPosition,
        StructureConfig.Mine.getLevelLayout(level).workplace().position(),
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
