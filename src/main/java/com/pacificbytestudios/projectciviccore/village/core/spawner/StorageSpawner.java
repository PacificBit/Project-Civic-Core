package com.pacificbytestudios.projectciviccore.village.core.spawner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3i;

import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.registry.registry.CropRecipeRegistry;
import com.pacificbytestudios.projectciviccore.registry.registry.FoodRecipeRegistry;
import com.pacificbytestudios.projectciviccore.registry.registry.SaplingRecipeRegistry;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
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

public class StorageSpawner {
  private static final String LIFE_ESSENCE_ITEM_ID = "Ingredient_Life_Essence";
  private static final int STARTER_LIFE_ESSENCE = 800;
  private static final int STARTER_FOOD_INGREDIENT_TOTAL = 100;
  private static final int STARTER_SEED_TOTAL = 100;
  private static final int STARTER_SAPLING_TOTAL = 100;

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
            "[handleNewStagedConstruction - storage] Registering new staged Storage construction of level: " +
                targetLevel + ", with id: " + structureId);

    WorldChunk chunkToUse = chunk;
    World world = chunkToUse.getWorld();
    short stage = (short) 0;
    String prefab = StructureConfig.Storage.getPrefab(targetLevel, stage);

    ProjectCivicCore.LOGGER.atInfo().log("[handleNewStagedConstruction - storage] Spawning prefab at: " + prefabAnchor);
    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        prefab,
        VectorUtil.clone(prefabAnchor),
        rotation,
        true);
    if (trackedPlacement == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[handleNewStagedConstruction - storage] Could not spawn prefab");
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
          .log("[handleNewStagedConstruction - storage] Could not fetch the construction sign entity");
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
      Vector3i existingCoreOrSitePosition,
      int targetLevel,
      ConstructionMode mode) {
    Pair<WorldChunk, Ref<ChunkStore>> result = Utils.getBlockComponentEntityChunkAware(
        chunk,
        existingCoreOrSitePosition);

    if (result == null || result.getSecond() == null ||
        !result.getSecond().isValid()) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[beginUpgradeConstruction - storage] Could not fetch the existing storage entity");
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
          .log(
              "[beginUpgradeConstruction - storage] Could not fetch the existing storage component or terrain snapshot");
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
        ProjectCivicCore.LOGGER.atSevere()
            .log("[beginUpgradeConstruction - storage] Could not complete the storage upgrade, rolling back");
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
      ProjectCivicCore.LOGGER.atSevere()
          .log("[beginUpgradeConstruction - storage] Could not fetch the existing workplace storage");
      return false;
    }

    SimpleItemContainer bufferedConstructionSignStorage = buildConstructionSignStorage(
        existingStorage.getContainers().size());
    ItemContainer constructionSignContainer = bufferedConstructionSignStorage;
    if (!moveWorkplaceStorageToTarget(chunkToUse, existingStorage, constructionSignContainer)) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[beginUpgradeConstruction - storage] Could not buffer the existing storage items");
      moveItemsToWorkplaceStorage(chunkToUse, constructionSignContainer, existingStorage);
      return false;
    }

    short stage = (short) 0;
    String prefab = StructureConfig.Storage.getPrefab(targetLevel, stage);
    TrackedPrefabPlacementComponent stagedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        prefab,
        VectorUtil.clone(prefabAnchor),
        structureRotation,
        true);

    if (stagedPlacement == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[beginUpgradeConstruction - storage] Could not spawn the first upgrade stage prefab");
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

    if (signResult == null || signResult.getSecond() == null || !signResult.getSecond().isValid()) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[beginUpgradeConstruction - storage] Could not fetch the upgrade construction sign entity");
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
      Vector3i prefabAnchor,
      Rotation rotation) {

    WorldChunk chunkToUse = chunk;
    Vector3i constructionSignPosition = getConstructionSignBlockPosition(prefabAnchor, rotation);
    Pair<WorldChunk, Ref<ChunkStore>> blockEntity = Utils.getBlockComponentEntityChunkAware(
        chunkToUse,
        constructionSignPosition);

    if (blockEntity == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[advanceConstruction - storage] Could not fetch the construction sign entity");
    }

    Ref<ChunkStore> constructionSignRef = blockEntity.getSecond();
    chunkToUse = blockEntity.getFirst();
    Store<ChunkStore> store = constructionSignRef.getStore();

    BuildingSnapshotComponent snapshotComponent = store.getComponent(
        constructionSignRef,
        BuildingSnapshotComponent.getComponentType());

    if (!removePreviousStagePrefab(chunk, store, constructionSignRef,
        constructionSignPosition, snapshotComponent)) {
      throw new IllegalStateException("[advanceConstruction - storage] Could not despawn previous prefab");
    }

    snapshotComponent.setCurrentStage((short) (snapshotComponent.getCurrentStage() + 1));

    if (snapshotComponent.getCurrentStage() == VillageObjectiveType.getStages(StructureType.STORAGE)) {
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
    String prefab = StructureConfig.Storage.getPrefab(snapshot.getTargetLevel(),
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

    String prefab = StructureConfig.Storage.getPrefab(snapshot.getTargetLevel(),
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
    String prefab = StructureConfig.Storage.getPrefab(
        targetLevel,
        VillageObjectiveType.getStages(StructureType.STORAGE));

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

    Vector3i workplaceBlockPosition = getWorkplaceBlockPosition(prefabAnchor, targetLevel, rotation);
    WorldChunk workplaceChunk = Utils.setBlockChunkAware(
        chunk,
        workplaceBlockPosition,
        StructureConfig.Storage.WORKPLACE_BLOCK,
        getWorkplaceBlockRotationIndex(targetLevel, rotation));
    Ref<ChunkStore> workplaceRef = workplaceChunk.getBlockComponentEntity(
        workplaceBlockPosition.x,
        workplaceBlockPosition.y,
        workplaceBlockPosition.z);

    if (workplaceRef == null || !workplaceRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[Storage - spawn] Workplace ref is invalid");
      return rollbackFailedSpawn(world, workplaceChunk, snapshotToUse, workplaceBlockPosition, null);
    }

    Store<ChunkStore> chunkStore = workplaceRef.getStore();
    WorkplaceComponent workplaceComponent = chunkStore.getComponent(
        workplaceRef,
        WorkplaceComponent.getComponentType());

    if (workplaceComponent == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[Storage - spawn] Workplace component is missing");
      return rollbackFailedSpawn(world, workplaceChunk, snapshotToUse, workplaceBlockPosition, null);
    }

    workplaceComponent.setUuid(structureId);
    workplaceComponent.setLevel((short) targetLevel);
    workplaceComponent.setBenchPos(workplaceBlockPosition);
    workplaceComponent.setBenchAccessPos(getBenchAccessPosition(workplaceBlockPosition, targetLevel, rotation));
    workplaceComponent.setWorkerType(new WorkType[] { WorkType.STORAGE_KEEPER });
    workplaceComponent.setPrefabAnchor(VectorUtil.clone(prefabAnchor));
    workplaceComponent.setWorkplaceRef(new EntityRef(chunk.getWorld().getName(), workplaceBlockPosition));

    chunkStore.putComponent(
        workplaceRef,
        TrackedPrefabPlacementComponent.getComponentType(),
        snapshotToUse);

    WorkplaceContainerRegistryComponent registry = spawnContainers(
        targetLevel, prefabAnchor, workplaceChunk, chunkStore, rotation);

    if (registry == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[Storage - spawn] Could not spawn correctly the containers");
      return rollbackFailedSpawn(world, workplaceChunk, snapshotToUse, workplaceBlockPosition, null);
    }

    workplaceComponent.setStorage(registry);
    moveConstructionSignItemsToStorage(chunk, prefabAnchor, targetLevel, rotation, registry);
    seedStarterStorageIfNeeded(chunk, registry, heart, targetLevel);
    removeConstructionSignIfPresent(chunk, prefabAnchor, targetLevel, rotation);
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
      ProjectCivicCore.LOGGER.atWarning()
          .log("[Storage - spawn] Could not move all starter items into the first level 1 storage");
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
      ProjectCivicCore.LOGGER.atWarning()
          .log("[Storage - spawn] No level 1 food recipes were found for starter storage");
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
      ProjectCivicCore.LOGGER.atWarning()
          .log("[Storage - spawn] No level 1 " + resourceType + " registry entries were found for starter storage");
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
      int level,
      Vector3i prefabAnchor,
      WorldChunk chunk,
      Store<ChunkStore> chunkStore,
      Rotation structureRotation) {
    WorkplaceContainerRegistryComponent registry = new WorkplaceContainerRegistryComponent();
    List<StructureConfig.ContainerTransform> layouts = StructureConfig.Storage.getLevelLayout(level).containers();
    WorldChunk usedChunk = chunk;
    Vector3i containerPosition;

    for (StructureConfig.ContainerTransform containerLayout : layouts) {
      Rotation containerRotation = StructurePlacementUtils.addYaw(
          containerLayout.rotation(),
          structureRotation);
      int rotation = RotationTuple.of(containerRotation, Rotation.None, Rotation.None).index();

      containerPosition = StructurePlacementUtils.withOffset(prefabAnchor, containerLayout.position(),
          structureRotation);

      usedChunk = Utils.setBlockChunkAware(
          usedChunk,
          containerPosition,
          StructureConfig.Storage.DOUBLE_STORAGE_BLOCK,
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
        ProjectCivicCore.LOGGER.atSevere()
            .log("[Storage - spawn] Could not resolve workplace storage container at " + entry.getPosition());
        return null;
      }

      Ref<ChunkStore> containerRef = result.getSecond();
      ItemContainerBlock containerBlock = containerRef.getStore().getComponent(
          containerRef,
          ItemContainerBlock.getComponentType());

      if (containerBlock == null) {
        ProjectCivicCore.LOGGER.atSevere()
            .log("[Storage - spawn] Could not fetch workplace storage container component at " + entry.getPosition());
        return null;
      }

      containers.add(containerBlock.getItemContainer());
    }

    return containers.toArray(new ItemContainer[0]);
  }

  private static void moveConstructionSignItemsToStorage(
      WorldChunk chunk,
      Vector3i prefabAnchor,
      int level,
      Rotation rotation,
      WorkplaceContainerRegistryComponent storageRegistry) {
    Vector3i constructionSignPosition = getConstructionSignBlockPosition(prefabAnchor, level, rotation);
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
        ProjectCivicCore.LOGGER.atWarning()
            .log("[Storage - spawn] Could not move all buffered construction items into the new storage containers");
      }
    }

    ItemContainerBlock signStorage = signStore.getComponent(
        signRef,
        ItemContainerBlock.getComponentType());

    if (signStorage != null && !signStorage.getItemContainer().isEmpty()) {
      movedAnyItems = true;
      if (!moveItemsToWorkplaceStorage(chunk, signStorage.getItemContainer(), storageRegistry)) {
        ProjectCivicCore.LOGGER.atWarning()
            .log("[Storage - spawn] Could not move all construction sign items into the new storage containers");
      }
    }

    if (movedAnyItems) {
      signChunk.markNeedsSaving();
    }
  }

  public static boolean despawnIncompleteBuilding(
      WorldChunk chunk,
      Vector3i prefabAnchor,
      int level,
      Rotation rotation) {
    if (chunk == null || prefabAnchor == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[despawnIncompleteBuilding - storage] Missing chunk or prefab anchor");
      return false;
    }

    Rotation structureRotation = StructurePlacementUtils.normalize(rotation);
    WorldChunk chunkToUse = chunk;
    Vector3i constructionSignPosition = getConstructionSignBlockPosition(prefabAnchor, level, structureRotation);
    Pair<WorldChunk, Ref<ChunkStore>> blockEntity = Utils.getBlockComponentEntityChunkAware(
        chunkToUse,
        constructionSignPosition);

    if (blockEntity == null || blockEntity.getSecond() == null || !blockEntity.getSecond().isValid()) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[despawnIncompleteBuilding - storage] Could not fetch the construction sign entity");
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
          StructureConfig.Storage.getPrefab(targetLevel, currentStage),
          targetAnchor,
          prefabRotation,
          true);
    }

    if (!despawn) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[despawnIncompleteBuilding - storage] Could not despawn storage prefab");
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
      ProjectCivicCore.LOGGER.atSevere().log("[Storage - despawn] Invalid workplaceRef");
      return false;
    }

    Store<ChunkStore> store = workplaceRef.getStore();

    WorkplaceComponent workplace = store.getComponent(workplaceRef,
        WorkplaceComponent.getComponentType());
    TrackedPrefabPlacementComponent trackedPlacement = store
        .getComponent(workplaceRef, TrackedPrefabPlacementComponent.getComponentType());

    if (workplace == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[Storage - despawn] Missing workplace component");
      return false;
    }

    Rotation prefabRotation = trackedPlacement != null ? trackedPlacement.getYaw() : Rotation.None;
    boolean despawn = false;
    if (trackedPlacement != null && trackedPlacement.hasRestoreSnapshot()) {
      despawn = PrefabManager.restoreTrackedPrefab(workplaceChunk.getWorld(), trackedPlacement);
    } else {
      Vector3i prefabAnchor = workplace.getPrefabAnchor();
      if (prefabAnchor == null && trackedPlacement != null) {
        prefabAnchor = trackedPlacement.getOrigin();
      }
      if (prefabAnchor == null) {
        prefabAnchor = getPrefabAnchorFromWorkplace(workplaceBlockPosition, workplace.getLevel(), prefabRotation);
      }

      despawn = PrefabManager.removePrefab(
          workplaceChunk.getWorld(),
          StructureConfig.Storage.getCompletedPrefab(workplace.getLevel()),
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
    Utils.breakBlockChunkAware(chunk, VectorUtil.clone(constructionPos).add(Vector3iUtil.UP));
  }

  private static int getWorkplaceBlockRotationIndex(int level, Rotation rotation) {
    Rotation workplaceRotation = StructurePlacementUtils.addYaw(
        StructureConfig.Storage.getLevelLayout(level).workplace().rotation(),
        rotation);
    return StructurePlacementUtils.toBlockRotationIndex(workplaceRotation);
  }

  private static int getConstructionSignRotationIndex(int level, Rotation rotation) {
    Rotation signRotation = StructurePlacementUtils.addYaw(
        StructureConfig.Storage.getLevelLayout(level).constructionSign().rotation(),
        rotation);
    return RotationTuple.of(signRotation, Rotation.None, Rotation.None).index();
  }

  private static Vector3i getWorkplaceBlockPosition(Vector3i prefabAnchor, int level, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        prefabAnchor,
        StructureConfig.Storage.getLevelLayout(level).workplace().position(),
        rotation);
  }

  private static Vector3i getConstructionSignBlockPosition(Vector3i prefabAnchor, int level, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        prefabAnchor,
        StructureConfig.Storage.getLevelLayout(level).constructionSign().position(),
        rotation);
  }

  private static Vector3i getConstructionSignBlockPosition(Vector3i prefabAnchor, Rotation rotation) {
    return getConstructionSignBlockPosition(prefabAnchor, 1, rotation);
  }

  private static Vector3i getBenchAccessPosition(Vector3i workplaceBlockPosition, int level, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        workplaceBlockPosition,
        StructureConfig.Storage.getLevelLayout(level).benchAccessOffset(),
        rotation);
  }

  private static Vector3i getContainerAccessPosition(
      Vector3i containerPosition,
      StructureConfig.ContainerTransform containerLayout,
      Rotation rotation) {
    return StructurePlacementUtils.withOffset(containerPosition, containerLayout.accessOffset(), rotation);
  }

  private static Vector3i getPrefabAnchorFromWorkplace(Vector3i workplaceBlockPosition, int level, Rotation rotation) {
    return StructurePlacementUtils.removeOffset(
        workplaceBlockPosition,
        StructureConfig.Storage.getLevelLayout(level).workplace().position(),
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
