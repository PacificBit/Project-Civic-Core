package com.pacificbytestudios.projectciviccore.village.core.spawner;

import java.util.UUID;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

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
import com.pacificbytestudios.projectciviccore.village.components.builder.BuildingSnapshotComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.AppartmentComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.VillageBedComponent;
import com.pacificbytestudios.projectciviccore.village.enums.ConstructionMode;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageObjectiveType;

import io.sentry.util.Pair;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class HouseSpawner {

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
      return handleNewCompletedConstruction(structureId, chunk, prefabAnchor,
          structureRotation, heart, targetLevel);
    }
    return handleNewStagedConstruction(structureId, chunk, prefabAnchor,
        structureRotation, heart, targetLevel);
  }

  private static boolean handleNewCompletedConstruction(
      UUID structureId,
      WorldChunk chunk,
      Vec3i prefabAnchor,
      Rotation structureRotation,
      VillageHeartComponent heart,
      int targetLevel) {
    return handleLastStageBuilding(
        structureId,
        chunk.getWorld(),
        chunk,
        prefabAnchor,
        structureRotation,
        heart,
        targetLevel,
        null);
  }

  private static boolean handleNewStagedConstruction(
      UUID structureId,
      WorldChunk chunk,
      Vec3i prefabAnchor,
      Rotation structureRotation,
      VillageHeartComponent heart,
      int targetLevel) {
    Logger.logInfo(
            "Registering new staged House construction of level: " +
                targetLevel + ", with id: " + structureId);

    WorldChunk chunkToUse = chunk;
    World world = chunkToUse.getWorld();
    short stage = (short) 0;
    String prefab = StructureConfig.House.getPrefab(targetLevel, stage);

    Logger.logInfo("Spawning prefab at: " + prefabAnchor);
    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        prefab,
        prefabAnchor.clone(),
        structureRotation,
        true);
    if (trackedPlacement == null) {
      Logger.logWarning("Could not spawn prefab");
      return false;
    }

    Vec3i constructionSignPosition = getConstructionSignBlockPosition(prefabAnchor, targetLevel, structureRotation);
    int constructionSignRotationIndex = getConstructionSignRotationIndex(targetLevel, structureRotation);
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
    snapshotComponent.setRotation(structureRotation);
    snapshotComponent.setStructureType(StructureType.HOUSE);
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
      Logger.logWarning("Could not fetch the existing house entity");
      return false;
    }

    WorldChunk chunkToUse = result.getFirst();
    Ref<ChunkStore> houseRef = result.getSecond();
    Store<ChunkStore> store = houseRef.getStore();

    HouseComponent house = store.getComponent(
        houseRef,
        HouseComponent.getComponentType());
    TrackedPrefabPlacementComponent trackedPlacement = store.getComponent(
        houseRef,
        TrackedPrefabPlacementComponent.getComponentType());
    if (house == null || trackedPlacement == null) {
      Logger.logWarning("Could not fetch the existing house component or terrain snapshot");
      return false;
    }

    Ref<ChunkStore> heartRef = house.getVillageRef().getRef();
    VillageHeartComponent heart = heartRef.getStore().getComponent(
        heartRef,
        VillageHeartComponent.getComponentType());

    UUID structureId = house.getUuid();
    int currentLevel = house.getLevel();
    Vec3i prefabAnchor = house.getPrefabAnchor().clone();
    Rotation structureRotation = StructurePlacementUtils.normalize(trackedPlacement.getYaw());
    EntityRef villageRefSnapshot = house.getVillageRef().clone();
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
        Logger.logWarning("Could not complete the house upgrade, rolling back");
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
      return upgraded;
    }

    short stage = (short) 0;
    String prefab = StructureConfig.House.getPrefab(targetLevel, stage);
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

    Vec3i constructionSignPosition = getConstructionSignBlockPosition(prefabAnchor, targetLevel, structureRotation);
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
    snapshotComponent.setStructureType(StructureType.HOUSE);
    snapshotComponent.setTargetLevel((short) targetLevel);
    snapshotComponent.setVillageRef(villageRefSnapshot);

    signStore.putComponent(
        constructionSignRef,
        TrackedPrefabPlacementComponent.getComponentType(),
        oldTrackedPlacement);

    signChunk.markNeedsSaving();
    return true;
  }

  public static boolean advanceConstruction(
      WorldChunk chunk,
      Vec3i prefabAnchor,
      Rotation rotation) {

    WorldChunk chunkToUse = chunk;
    Vec3i constructionSignPosition = getConstructionSignBlockPosition(prefabAnchor, rotation);
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
      throw new IllegalStateException("[advanceConstruction - house] Could not despawn previous prefab");
    }

    snapshotComponent.setCurrentStage((short) (snapshotComponent.getCurrentStage() + 1));

    if (snapshotComponent.getCurrentStage() == VillageObjectiveType.getStages(StructureType.HOUSE)) {
      VillageHeartComponent heart = store.getComponent(
          snapshotComponent.getVillageRef().getRef(),
          VillageHeartComponent.getComponentType());

      // NOTE: this contains the world snapshot before the first ever prefab
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
      Vec3i constructionSignPosition,
      BuildingSnapshotComponent snapshot) {
    TrackedPrefabPlacementComponent trackedPlacement = store
        .getComponent(constructionSignRef,
            TrackedPrefabPlacementComponent.getComponentType());

    Rotation prefabRotation = trackedPlacement != null ? trackedPlacement.getYaw() : Rotation.None;
    boolean despawn = false;
    String prefab = StructureConfig.House.getPrefab(snapshot.getTargetLevel(),
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
      Vec3i constructionSignPosition,
      BuildingSnapshotComponent snapshot) {

    String prefab = StructureConfig.House.getPrefab(snapshot.getTargetLevel(),
        snapshot.getCurrentStage());

    // NOTE: I'm not saving the new snapshot to keep the first ever snapshot
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
      Vec3i prefabAnchor,
      Rotation rotation,
      VillageHeartComponent heart,
      int targetLevel,
      TrackedPrefabPlacementComponent trackedPlacement) {
    String prefab = StructureConfig.House.getPrefab(
        targetLevel,
        VillageObjectiveType.getStages(StructureType.HOUSE));

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
            StructureConfig.House.getCoreBlockOffset(targetLevel).rotation(),
            rotation),
        Rotation.None,
        Rotation.None).index();

    Vec3i houseCoreBlockPosition = getWorkplaceBlockPosition(
        prefabAnchor,
        targetLevel,
        rotation);
    WorldChunk houseChunk = Utils.setBlockChunkAware(
        chunk,
        houseCoreBlockPosition,
        StructureConfig.House.CORE_BLOCK, blockRotation);
    Ref<ChunkStore> houseCoreRef = houseChunk.getBlockComponentEntity(
        houseCoreBlockPosition.x,
        houseCoreBlockPosition.y,
        houseCoreBlockPosition.z);

    if (houseCoreRef == null || !houseCoreRef.isValid()) {
      Logger.logWarning("House ref is invalid");
      return rollbackFailedSpawn(world, houseChunk, snapshotToUse,
          houseCoreBlockPosition, null);
    }

    Store<ChunkStore> chunkStore = houseCoreRef.getStore();

    HouseComponent house = chunkStore.getComponent(houseCoreRef,
        HouseComponent.getComponentType());
    if (house == null) {
      Logger.logWarning("Could not fetch house component");
      return rollbackFailedSpawn(world, houseChunk, snapshotToUse,
          houseCoreBlockPosition, null);
    }

    house.setLevel((short) targetLevel);
    house.setUuid(structureId);
    house.setCoreBlockPosition(houseCoreBlockPosition);
    house.setPrefabAnchor(prefabAnchor.clone());
    house.setHouseRef(new EntityRef(chunk.getWorld().getName(),
        houseCoreBlockPosition));
    house.setAppartmentRegistry(StructureConfig.House.getAppartments(prefabAnchor,
        rotation, targetLevel));

    spawnBeds(world, chunk, house);

    chunkStore.putComponent(
        houseCoreRef,
        TrackedPrefabPlacementComponent.getComponentType(),
        snapshotToUse);

    removeConstructionSignIfPresent(chunk, prefabAnchor, targetLevel, rotation);
    houseChunk.markNeedsSaving();
    return true;
  }

  private static void spawnBeds(World world, WorldChunk chunk, HouseComponent house) {
    for (AppartmentComponent appartment : house.getAppartmentRegistry().getAppartments().values()) {
      for (VillageBedComponent bed : appartment.getBeds().values()) {
        Utils.setBlockChunkAware(
            chunk,
            bed.getPosition(),
            StructureConfig.House.getBedBlockPerLevel(house.getLevel()),
            StructurePlacementUtils.toBlockRotationIndex(bed.getRotation()));
        Utils.markNeedSaving(world, bed.getPosition());
      }
    }
  }

  public static boolean despawnIncompleteBuilding(
      WorldChunk chunk,
      Vec3i prefabAnchor,
      int level,
      Rotation rotation) {
    if (chunk == null || prefabAnchor == null) {
      Logger.logWarning("Missing chunk or prefab anchor");
      return false;
    }

    Rotation structureRotation = StructurePlacementUtils.normalize(rotation);
    WorldChunk chunkToUse = chunk;
    Vec3i constructionSignPosition = getConstructionSignBlockPosition(prefabAnchor, level, structureRotation);
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
          StructureConfig.House.getPrefab(targetLevel, currentStage),
          targetAnchor,
          prefabRotation,
          true);
    }

    if (!despawn) {
      Logger.logWarning("Could not despawn house prefab");
      return false;
    }

    removeConstructionSignIfPresent(chunkToUse, targetAnchor, targetLevel, structureRotation);
    return true;
  }

  public static boolean despawn(WorldChunk chunk, Vec3i position) {
    Vec3i bedBlockPosition = position.clone();
    Pair<WorldChunk, Ref<ChunkStore>> result = Utils.getBlockComponentEntityChunkAware(
        chunk,
        bedBlockPosition);

    WorldChunk bedChunk = result.getFirst();
    Ref<ChunkStore> bedRef = result.getSecond();

    if (bedRef == null || !bedRef.isValid()) {
      Logger.logWarning("Invalid workplaceRef");
      return false;
    }

    Store<ChunkStore> store = bedRef.getStore();

    HouseComponent component = store.getComponent(bedRef,
        HouseComponent.getComponentType());
    TrackedPrefabPlacementComponent trackedPlacement = store
        .getComponent(bedRef, TrackedPrefabPlacementComponent.getComponentType());

    Rotation prefabRotation = trackedPlacement != null ? trackedPlacement.getYaw() : Rotation.None;
    boolean despawn = false;
    if (trackedPlacement != null && trackedPlacement.hasRestoreSnapshot()) {
      despawn = PrefabManager.restoreTrackedPrefab(bedChunk.getWorld(),
          trackedPlacement);
    } else {
      Vec3i prefabAnchor = component.getPrefabAnchor();
      if (prefabAnchor == null && trackedPlacement != null) {
        prefabAnchor = trackedPlacement.getOrigin();
      }
      if (prefabAnchor == null) {
        prefabAnchor = getPrefabAnchorFromWorkplace(bedBlockPosition,
            component.getLevel(),
            prefabRotation);
      }

      despawn = PrefabManager.removePrefab(
          bedChunk.getWorld(),
          StructureConfig.House.getCompletedPrefab(component.getLevel()),
          prefabAnchor,
          prefabRotation,
          true);
    }

    if (!despawn) {
      Logger.logWarning("Could not despawn farm prefab");
      return false;
    }

    Utils.breakBlockChunkAware(bedChunk, bedBlockPosition);
    return despawn;
  }

  private static void removeConstructionSignIfPresent(
      WorldChunk chunk,
      Vec3i prefabAnchor,
      int level,
      Rotation rotation) {
    Vec3i constructionSignPosition = getConstructionSignBlockPosition(prefabAnchor, level, rotation);
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

  private static void clearArtefactBlocks(WorldChunk chunk, Vec3i constructionPos) {
    Utils.breakBlockChunkAware(chunk, constructionPos);
    Utils.breakBlockChunkAware(chunk, constructionPos.clone().add(new Vec3i(0, 1, 0)));
  }

  private static int getConstructionSignRotationIndex(int level, Rotation rotation) {
    Rotation signRotation = StructurePlacementUtils.addYaw(
        StructureConfig.House.getLevelLayout(level).constructionSign().rotation(),
        rotation);
    return RotationTuple.of(signRotation, Rotation.None, Rotation.None).index();
  }

  private static Vec3i getWorkplaceBlockPosition(
      Vec3i prefabAnchor,
      int level,
      Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        prefabAnchor,
        StructureConfig.House.getCoreBlockOffset(level).position(),
        rotation);
  }

  private static Vec3i getConstructionSignBlockPosition(Vec3i prefabAnchor, int level, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        prefabAnchor,
        StructureConfig.House.getLevelLayout(level).constructionSign().position(),
        rotation);
  }

  private static Vec3i getConstructionSignBlockPosition(Vec3i prefabAnchor, Rotation rotation) {
    return getConstructionSignBlockPosition(prefabAnchor, 1, rotation);
  }

  private static Vec3i getPrefabAnchorFromWorkplace(
      Vec3i workplaceBlockPosition,
      int targetLevel,
      Rotation rotation) {
    return StructurePlacementUtils.removeOffset(
        workplaceBlockPosition,
        StructureConfig.House.getCoreBlockOffset(targetLevel).position(),
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
