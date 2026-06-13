package com.pacificbytestudios.projectciviccore.village.core.spawner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform3D;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.StructureLevelConfig;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.registry.registry.CropRecipeRegistry;
import com.pacificbytestudios.projectciviccore.registry.registry.FoodRecipeRegistry;
import com.pacificbytestudios.projectciviccore.registry.registry.SaplingRecipeRegistry;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.builder.BufferedConstructionStorageComponent;
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

public class StorageSpawner {
  private static final String LIFE_ESSENCE_ITEM_ID = "Ingredient_Life_Essence";
  private static final int STARTER_LIFE_ESSENCE = 800;
  private static final int STARTER_FOOD_INGREDIENT_TOTAL = 100;
  private static final int STARTER_SEED_TOTAL = 100;
  private static final int STARTER_SAPLING_TOTAL = 100;

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
        "Registering new staged Storage construction of level %d, with id: %s",
        targetLevel, structureId);

    WorldChunk chunkToUse = chunk;
    World world = chunkToUse.getWorld();
    short stage = (short) 0;
    StructureLevelConfig config = StructureSpawnConfig.get(heart, StructureType.STORAGE);
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
    snapshotComponent.setStructureType(StructureType.STORAGE);
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
      Logger.logWarning("Could not fetch the existing storage entity");
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
          "Could not fetch the existing storage component or terrain snapshot");
      return false;
    }

    Ref<ChunkStore> heartRef = workplace.getVillageHeartRef();
    VillageHeartComponent heart = heartRef.getStore().getComponent(
        heartRef,
        VillageHeartComponent.getComponentType());
    StructureLevelConfig config = StructureSpawnConfig.get(heart, StructureType.STORAGE);

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
        Logger.logWarning("Could not complete the storage upgrade, rolling back");
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
        VillageStorageRequestInvalidation.invalidateTargetStorage(heartRef, heart, structureId);
      }
      return upgraded;
    }

    WorkplaceContainerRegistryComponent existingStorage = workplace.getStorage() != null
        ? workplace.getStorage().clone()
        : null;
    if (existingStorage == null || existingStorage.getContainers().isEmpty()) {
      Logger.logWarning("Could not fetch the existing workplace storage");
      return false;
    }

    SimpleItemContainer bufferedConstructionSignStorage = buildConstructionSignStorage(
        existingStorage.getContainers().size());
    ItemContainer constructionSignContainer = bufferedConstructionSignStorage;
    if (!moveWorkplaceStorageToTarget(chunkToUse, existingStorage, constructionSignContainer)) {
      Logger.logWarning("Could not buffer the existing storage items");
      moveItemsToWorkplaceStorage(chunkToUse, constructionSignContainer, existingStorage);
      return false;
    }

    short stage = (short) 0;
    String prefab = StructureSpawnConfig.getPrefab(config, targetLevel, stage);
    TrackedPrefabPlacementComponent stagedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        prefab,
        prefabAnchor,
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
      moveItemsToWorkplaceStorage(chunkToUse, constructionSignContainer, existingStorage);
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

    if (signResult == null || signResult.getSecond() == null || !signResult.getSecond().isValid()) {
      Logger.logWarning("Could not fetch the upgrade construction sign entity");
      Utils.breakBlockChunkAware(signChunk, constructionSignPosition);
      handleLastStageBuilding(
          structureId,
          world,
          chunkToUse,
          prefabAnchor,
          structureRotation,
          heart,
          currentLevel,
          oldTrackedPlacement);
      moveItemsToWorkplaceStorage(chunkToUse, constructionSignContainer, existingStorage);
      return false;
    }

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
    snapshotComponent.setStructureType(StructureType.STORAGE);
    snapshotComponent.setTargetLevel((short) targetLevel);
    snapshotComponent.setVillageRef(villageRefSnapshot);

    BufferedConstructionStorageComponent bufferedStorageComponent = new BufferedConstructionStorageComponent();
    bufferedStorageComponent.setItemContainer(bufferedConstructionSignStorage);
    signStore.putComponent(
        constructionSignRef,
        BufferedConstructionStorageComponent.getComponentType(),
        bufferedStorageComponent);

    signStore.putComponent(
        constructionSignRef,
        TrackedPrefabPlacementComponent.getComponentType(),
        oldTrackedPlacement);

    signChunk.markNeedsSaving();
    VillageStorageRequestInvalidation.invalidateRequesterWorkplace(heartRef, heart, structureId);
    VillageStorageRequestInvalidation.invalidateTargetStorage(heartRef, heart, structureId);
    return true;
  }

  public static boolean advanceConstruction(
      WorldChunk chunk,
      Vec3i prefabAnchor,
      Rotation rotation,
      VillageHeartComponent heart) {

    WorldChunk chunkToUse = chunk;
    StructureLevelConfig config = StructureSpawnConfig.get(heart, StructureType.STORAGE);
    Pair<WorldChunk, Ref<ChunkStore>> blockEntity = StructureSpawnConfig.findConstructionSign(
        chunkToUse,
        prefabAnchor,
        rotation,
        config,
        StructureType.STORAGE);

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
      throw new IllegalStateException("[advanceConstruction - storage] Could not despawn previous prefab");
    }

    snapshotComponent.setCurrentStage((short) (snapshotComponent.getCurrentStage() + 1));

    if (snapshotComponent.getCurrentStage() == VillageObjectiveType.getStages(StructureType.STORAGE)) {
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
    StructureLevelConfig config = StructureSpawnConfig.get(heart, StructureType.STORAGE);
    String prefab = StructureSpawnConfig.getCompletedPrefab(
        config,
        StructureType.STORAGE,
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

    Vec3i workplaceBlockPosition = getWorkplaceBlockPosition(config, prefabAnchor, targetLevel, rotation);
    WorldChunk workplaceChunk = Utils.setBlockChunkAware(
        chunk,
        workplaceBlockPosition,
        StructureConfig.Storage.WORKPLACE_BLOCK,
        getWorkplaceBlockRotationIndex(config, targetLevel, rotation));
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
    workplaceComponent.setWorkerType(new WorkType[] { WorkType.STORAGE_KEEPER });
    workplaceComponent.setPrefabAnchor(prefabAnchor);
    workplaceComponent.setWorkplaceRef(new EntityRef(chunk.getWorld().getName(), workplaceBlockPosition));

    chunkStore.putComponent(
        workplaceRef,
        TrackedPrefabPlacementComponent.getComponentType(),
        snapshotToUse);

    WorkplaceContainerRegistryComponent registry = spawnContainers(
        config, targetLevel, prefabAnchor, workplaceChunk, chunkStore, rotation);

    if (registry == null) {
      Logger.logWarning("Could not spawn correctly the containers");
      return rollbackFailedSpawn(world, workplaceChunk, snapshotToUse, workplaceBlockPosition, null);
    }

    workplaceComponent.setStorage(registry);
    StructureSpawnConfig.applyConfigIds(workplaceComponent, config, targetLevel);
    moveConstructionSignItemsToStorage(config, chunk, prefabAnchor, targetLevel, rotation, registry);
    seedStarterStorageIfNeeded(chunk, registry, heart, targetLevel);
    removeConstructionSignIfPresent(config, chunk, prefabAnchor, targetLevel, rotation);
    workplaceChunk.markNeedsSaving();
    return true;
  }

  private static void seedStarterStorageIfNeeded(
      WorldChunk chunk,
      WorkplaceContainerRegistryComponent storageRegistry,
      VillageHeartComponent heart,
      int targetLevel) {
    if (targetLevel != 1 || heart == null || heart.getStorageWorkplaceCount() != 0) {
      return;
    }

    SimpleItemContainer starterItems = buildStarterStorageItems();
    if (starterItems == null || starterItems.isEmpty()) {
      return;
    }

    if (!moveItemsToWorkplaceStorage(chunk, starterItems, storageRegistry)) {
      Logger.logWarning("Could not move all starter items into the first level 1 storage");
    }
  }

  private static SimpleItemContainer buildStarterStorageItems() {
    List<ItemStack> starterStacks = new ArrayList<>();
    starterStacks.add(new ItemStack(LIFE_ESSENCE_ITEM_ID, STARTER_LIFE_ESSENCE));

    appendDistributedStacks(
        starterStacks,
        new ArrayList<>(CropRecipeRegistry.getLevelDependantCraftingRecipe(1).keySet()),
        STARTER_SEED_TOTAL,
        "seed");
    appendDistributedStacks(
        starterStacks,
        new ArrayList<>(SaplingRecipeRegistry.getLevelDependantSaplingRecipe(1).keySet()),
        STARTER_SAPLING_TOTAL,
        "sapling");
    appendFoodIngredientStacks(starterStacks, STARTER_FOOD_INGREDIENT_TOTAL);

    if (starterStacks.isEmpty()) {
      return null;
    }

    short capacity = calculateRequiredStarterStorageCapacity(starterStacks);
    SimpleItemContainer starterItems = new SimpleItemContainer(capacity);
    for (ItemStack stack : starterStacks) {
      starterItems.addItemStack(stack);
    }
    return starterItems;
  }

  private static short calculateRequiredStarterStorageCapacity(List<ItemStack> starterStacks) {
    int requiredSlots = 0;

    for (ItemStack stack : starterStacks) {
      if (stack == null || stack.isEmpty()) {
        continue;
      }

      int maxStack = Math.max(1, stack.getItem().getMaxStack());
      requiredSlots += Math.max(1, (stack.getQuantity() + maxStack - 1) / maxStack);
    }

    return (short) Math.max(9, requiredSlots);
  }

  private static void appendFoodIngredientStacks(List<ItemStack> target, int totalQuantity) {
    Map<String, CraftingRecipe> levelRecipes = FoodRecipeRegistry.getLevelDependantCraftingRecipe(1);
    if (levelRecipes == null || levelRecipes.isEmpty()) {
      Logger.logWarning("No level 1 food recipes were found for starter storage");
      return;
    }

    Set<String> ingredientIds = new HashSet<>();
    for (CraftingRecipe recipe : levelRecipes.values()) {
      if (recipe == null) {
        continue;
      }

      for (MaterialQuantity material : CraftingManager.getInputMaterials(recipe, 1)) {
        if (material == null || material.getItemId() == null || material.getItemId().isBlank()) {
          continue;
        }

        ingredientIds.add(material.getItemId());
      }
    }

    appendDistributedStacks(target, new ArrayList<>(ingredientIds), totalQuantity, "food ingredient");
  }

  private static void appendDistributedStacks(
      List<ItemStack> target,
      List<String> itemIds,
      int totalQuantity,
      String resourceType) {
    if (itemIds == null || itemIds.isEmpty()) {
      Logger.logWarning("No level 1 " + resourceType + " registry entries were found for starter storage");
      return;
    }

    itemIds.sort(String::compareTo);

    int baseQuantity = totalQuantity / itemIds.size();
    int remainder = totalQuantity % itemIds.size();

    for (int index = 0; index < itemIds.size(); index++) {
      int quantity = baseQuantity + (index < remainder ? 1 : 0);
      if (quantity <= 0) {
        continue;
      }

      target.add(new ItemStack(itemIds.get(index), quantity));
    }
  }

  private static WorkplaceContainerRegistryComponent spawnContainers(
      StructureLevelConfig config,
      int level,
      Vec3i prefabAnchor,
      WorldChunk chunk,
      Store<ChunkStore> chunkStore,
      Rotation structureRotation) {
    WorkplaceContainerRegistryComponent registry = new WorkplaceContainerRegistryComponent();
    BlockTransform3D[] layouts = config.getContainersFor((short) level);
    WorldChunk usedChunk = chunk;
    Vec3i containerPosition;

    for (BlockTransform3D containerLayout : layouts) {
      int rotation = StructureSpawnConfig.blockRotationIndex(containerLayout, structureRotation);

      containerPosition = StructureSpawnConfig.blockPosition(
          prefabAnchor,
          containerLayout,
          structureRotation);

      usedChunk = Utils.setBlockChunkAware(
          usedChunk,
          containerPosition,
          StructureConfig.Storage.DOUBLE_STORAGE_BLOCK,
          rotation);

      Ref<ChunkStore> containerRef = usedChunk.getBlockComponentEntity(containerPosition.x,
          containerPosition.y, containerPosition.z);

      if (containerRef == null || !containerRef.isValid()) {
        Logger.logWarning("Container ref is invalid");
        return null;
      }

      WorkplaceContainerEntryComponent container = chunkStore.getComponent(
          containerRef,
          WorkplaceContainerEntryComponent.getComponentType());

      if (container == null) {
        Logger.logWarning("Container component is missing");
        return null;
      }

      container.setPosition(containerPosition);
      container.setAccessOffset(getContainerAccessOffset(containerLayout, structureRotation));
      container.setUuid(UUID.randomUUID());
      registry.registerContainer(container.getUuid(), container);
    }

    return registry;
  }

  private static SimpleItemContainer buildConstructionSignStorage(int workplaceStorageCount) {
    short capacity = (short) (9 * 4 * workplaceStorageCount);
    return new SimpleItemContainer(capacity);
  }

  private static boolean moveWorkplaceStorageToTarget(
      WorldChunk chunk,
      WorkplaceContainerRegistryComponent storageRegistry,
      ItemContainer target) {
    ItemContainer[] storageContainers = resolveWorkplaceStorageContainers(chunk, storageRegistry);
    if (storageContainers == null) {
      return false;
    }

    for (ItemContainer storageContainer : storageContainers) {
      storageContainer.moveAllItemStacksTo(target);
      if (!storageContainer.isEmpty()) {
        return false;
      }
    }

    return true;
  }

  private static boolean moveItemsToWorkplaceStorage(
      WorldChunk chunk,
      ItemContainer source,
      WorkplaceContainerRegistryComponent storageRegistry) {
    ItemContainer[] storageContainers = resolveWorkplaceStorageContainers(chunk, storageRegistry);
    if (source == null || storageContainers == null || storageContainers.length == 0) {
      return false;
    }

    source.moveAllItemStacksTo(storageContainers);
    return source.isEmpty();
  }

  private static ItemContainer[] resolveWorkplaceStorageContainers(
      WorldChunk chunk,
      WorkplaceContainerRegistryComponent storageRegistry) {
    if (storageRegistry == null || storageRegistry.getContainers().isEmpty()) {
      return null;
    }

    List<ItemContainer> containers = new ArrayList<>(storageRegistry.getContainers().size());

    for (WorkplaceContainerEntryComponent entry : storageRegistry.getContainers().values()) {
      Pair<WorldChunk, Ref<ChunkStore>> result = Utils.getBlockComponentEntityChunkAware(
          chunk,
          entry.getPosition());

      if (result == null || result.getSecond() == null || !result.getSecond().isValid()) {
        Logger.logWarning("Could not resolve workplace storage container at " + entry.getPosition());
        return null;
      }

      Ref<ChunkStore> containerRef = result.getSecond();
      ItemContainerBlock containerBlock = containerRef.getStore().getComponent(
          containerRef,
          ItemContainerBlock.getComponentType());

      if (containerBlock == null) {
        Logger.logWarning("Could not fetch workplace storage container component at " + entry.getPosition());
        return null;
      }

      containers.add(containerBlock.getItemContainer());
    }

    return containers.toArray(new ItemContainer[0]);
  }

  private static void moveConstructionSignItemsToStorage(
      StructureLevelConfig config,
      WorldChunk chunk,
      Vec3i prefabAnchor,
      int level,
      Rotation rotation,
      WorkplaceContainerRegistryComponent storageRegistry) {
    Vec3i constructionSignPosition = getConstructionSignBlockPosition(config, prefabAnchor, level, rotation);
    Pair<WorldChunk, Ref<ChunkStore>> signResult = Utils.getBlockComponentEntityChunkAware(
        chunk,
        constructionSignPosition);

    if (signResult == null || signResult.getSecond() == null || !signResult.getSecond().isValid()) {
      return;
    }

    WorldChunk signChunk = signResult.getFirst();
    Ref<ChunkStore> signRef = signResult.getSecond();
    Store<ChunkStore> signStore = signRef.getStore();
    boolean movedAnyItems = false;

    BufferedConstructionStorageComponent bufferedStorage = signStore.getComponent(
        signRef,
        BufferedConstructionStorageComponent.getComponentType());
    if (bufferedStorage != null
        && bufferedStorage.getItemContainer() != null
        && !bufferedStorage.getItemContainer().isEmpty()) {
      movedAnyItems = true;
      if (!moveItemsToWorkplaceStorage(chunk, bufferedStorage.getItemContainer(), storageRegistry)) {
        Logger.logWarning("Could not move all buffered construction items into the new storage containers");
      }
    }

    ItemContainerBlock signStorage = signStore.getComponent(
        signRef,
        ItemContainerBlock.getComponentType());

    if (signStorage != null && !signStorage.getItemContainer().isEmpty()) {
      movedAnyItems = true;
      if (!moveItemsToWorkplaceStorage(chunk, signStorage.getItemContainer(), storageRegistry)) {
        Logger.logWarning("Could not move all construction sign items into the new storage containers");
      }
    }

    if (movedAnyItems) {
      signChunk.markNeedsSaving();
    }
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
    StructureLevelConfig config = StructureSpawnConfig.get(heart, StructureType.STORAGE);
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
      Logger.logWarning("Could not despawn storage prefab");
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

    WorkplaceComponent workplace = store.getComponent(workplaceRef,
        WorkplaceComponent.getComponentType());
    TrackedPrefabPlacementComponent trackedPlacement = store
        .getComponent(workplaceRef, TrackedPrefabPlacementComponent.getComponentType());

    if (workplace == null) {
      Logger.logWarning("Missing workplace component");
      return false;
    }
    VillageHeartComponent heart = workplace.getVillageHeartRef().getStore().getComponent(
        workplace.getVillageHeartRef(),
        VillageHeartComponent.getComponentType());
    StructureLevelConfig config = StructureSpawnConfig.get(heart, StructureType.STORAGE);

    Rotation prefabRotation = trackedPlacement != null ? trackedPlacement.getYaw() : Rotation.None;
    boolean despawn = false;
    if (trackedPlacement != null && trackedPlacement.hasRestoreSnapshot()) {
      despawn = PrefabManager.restoreTrackedPrefab(workplaceChunk.getWorld(), trackedPlacement);
    } else {
      Vec3i prefabAnchor = workplace.getPrefabAnchor() == null ? null : workplace.getPrefabAnchor();
      if (prefabAnchor == null && trackedPlacement != null) {
        prefabAnchor = trackedPlacement.getOrigin();
      }
      if (prefabAnchor == null) {
        prefabAnchor = getPrefabAnchorFromWorkplace(config, workplaceBlockPosition, workplace.getLevel(),
            prefabRotation);
      }

      despawn = PrefabManager.removePrefab(
          workplaceChunk.getWorld(),
          StructureSpawnConfig.getCompletedPrefab(config, StructureType.STORAGE, workplace.getLevel()),
          prefabAnchor,
          prefabRotation,
          true);
    }

    if (!despawn) {
      Logger.logWarning("Could not despawn storage prefab");
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

  private static Vec3f getBenchAccessOffset(
      StructureLevelConfig config,
      int level,
      Rotation rotation) {
    return StructureSpawnConfig.accessOffset(
        StructureSpawnConfig.getCoreBlock(config, level),
        rotation);
  }

  private static Vec3f getContainerAccessOffset(
      BlockTransform3D containerLayout,
      Rotation rotation) {
    return StructureSpawnConfig.accessOffset(containerLayout, rotation);
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
