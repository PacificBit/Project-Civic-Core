package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.innkeeper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.InternalContainerUtilMaterial;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.food.registry.RecipeRegistry;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest.ItemRequestCriterion;
import com.pacificbytestudios.projectciviccore.village.components.workplace.tavern.TavernComponent;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.innkeeper.BuilderCreateNewMealPlanAction;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver.ResolvedContainer;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;

public class CreateNewMealPlanAction extends VillagerActionBase {
  private final int successFlagSlot;
  private final int failureFlagSlot;

  public CreateNewMealPlanAction(BuilderCreateNewMealPlanAction actionBuilder, BuilderSupport support) {
    super(actionBuilder);
    this.successFlagSlot = actionBuilder.getSuccessFlagSlot(support);
    this.failureFlagSlot = actionBuilder.getFailureFlagSlot(support);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider infoProvider,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.canExecute(ref, role, infoProvider, deltaTime, store)) {
      return false;
    }
    super.execute(ref, role, infoProvider, deltaTime, store);

    Ref<ChunkStore> workplaceRef = this.getWorkplaceRef(ref, store);
    VillageHeartComponent villageHeart = this.getVillageHeartComponent(ref, store);
    WorkplaceComponent workplaceComponent = this.getWorkplaceComponent(ref, store);
    if (workplaceRef == null || villageHeart == null || workplaceComponent == null) {
      return this.finish(role, false);
    }

    World world = store.getExternalData().getWorld();
    Store<ChunkStore> chunkStore = world.getChunkStore().getStore();

    TavernComponent tavernComponent = chunkStore.getComponent(workplaceRef, TavernComponent.getComponentType());
    if (tavernComponent == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[CreateNewMealPlanAction] Could not fetch the tavern component");
      return this.finish(role, false);
    }

    int currentVillageLevel = villageHeart.getLevel();
    int totalVillagers = villageHeart.getVillagerCount();

    if (tavernComponent.getMealToPrepareCount() >= totalVillagers) {
      ProjectCivicCore.LOGGER.atInfo()
          .log("[CreateNewMealPlanAction] Meal plan already present");
      return this.finish(role, true);
    }

    System.out.println("=========================================================");
    System.out.println("CREO UN NUOVO MEAL PLAN");
    System.out.println("=========================================================");
    tavernComponent.setMealToPrepare(new HashMap<>());
    tavernComponent.resetMissingResources();

    List<ItemContainer> storageSnapshots = new ArrayList<>();
    for (Vector3i storagePosition : workplaceComponent.getStorage().getAllContainerPositions()) {
      ResolvedContainer resolvedStorage = ContainerResolver.resolveContainerAt(world, storagePosition);
      if (resolvedStorage == null) {
        ProjectCivicCore.LOGGER.atSevere()
            .log("[CreateNewMealPlanAction] Dead reference of a container is present at " + storagePosition);
        return this.finish(role, false);
      }

      SimpleItemContainer containerSnapshot = resolvedStorage.containerBlock().getItemContainer().clone();
      storageSnapshots.add(containerSnapshot);
    }

    CombinedItemContainer combinedSnapshot = new CombinedItemContainer(storageSnapshots.toArray(new ItemContainer[0]));
    populateMealPlanFromSnapshot(combinedSnapshot, currentVillageLevel, tavernComponent, totalVillagers);

    int missingMealCount = totalVillagers - tavernComponent.getMealToPrepareCount();
    if (missingMealCount > 0) {
      Map<String, Integer> missingMaterials = new HashMap<>();
      populateMissingMaterialsForMealPlan(combinedSnapshot, currentVillageLevel, missingMealCount, missingMaterials);
      missingMaterials.forEach((itemId, amount) -> tavernComponent
          .addMissingResource(new ItemRequestCriterion(itemId, true, amount)));
    }

    System.out.println("Created meal plan: ");
    System.out.println(tavernComponent.getMealToPrepare());
    System.out.println("Meal count: ");
    System.out.println(tavernComponent.getMealToPrepareCount());
    System.out.println("Missing resources: ");
    System.out.println(tavernComponent.getMissingResources());

    return this.finish(role, tavernComponent.getMealToPrepareCount() >= totalVillagers);
  }

  private boolean finish(Role role, boolean success) {
    if (this.successFlagSlot >= 0) {
      role.setFlag(this.successFlagSlot, success);
    }
    if (this.failureFlagSlot >= 0) {
      role.setFlag(this.failureFlagSlot, !success);
    }
    return success;
  }

  private void populateMealPlanFromSnapshot(
      ItemContainer containerSnapshot,
      int recipeLevel,
      TavernComponent tavernComponent,
      int targetRecipeCount) {
    if (recipeLevel <= 0 || tavernComponent.getMealToPrepareCount() >= targetRecipeCount) {
      return;
    }

    List<Map.Entry<String, CraftingRecipe>> orderedRecipes = getOrderedRecipes(recipeLevel);
    boolean craftedAnyRecipeInRound;
    do {
      craftedAnyRecipeInRound = false;

      for (Map.Entry<String, CraftingRecipe> recipeEntry : orderedRecipes) {
        if (tavernComponent.getMealToPrepareCount() >= targetRecipeCount) {
          return;
        }

        List<MaterialQuantity> neededMaterials = CraftingManager.getInputMaterials(recipeEntry.getValue(), 1);
        if (!containerSnapshot.canRemoveMaterials(neededMaterials)) {
          continue;
        }

        containerSnapshot.removeMaterials(neededMaterials, true, true, true);
        tavernComponent.addMealToPrepare(recipeEntry.getKey(), 1);
        craftedAnyRecipeInRound = true;
      }
    } while (craftedAnyRecipeInRound && tavernComponent.getMealToPrepareCount() < targetRecipeCount);

    populateMealPlanFromSnapshot(
        containerSnapshot,
        recipeLevel - 1,
        tavernComponent,
        targetRecipeCount);
  }

  private void populateMissingMaterialsForMealPlan(
      ItemContainer containerSnapshot,
      int recipeLevel,
      int missingMealCount,
      Map<String, Integer> missingMaterials) {
    if (recipeLevel <= 0 || missingMealCount <= 0) {
      return;
    }

    List<Map.Entry<String, CraftingRecipe>> orderedRecipes = getOrderedRecipes(recipeLevel);
    if (orderedRecipes.isEmpty()) {
      populateMissingMaterialsForMealPlan(containerSnapshot, recipeLevel - 1, missingMealCount, missingMaterials);
      return;
    }

    while (missingMealCount > 0) {
      for (Map.Entry<String, CraftingRecipe> recipeEntry : orderedRecipes) {
        if (missingMealCount <= 0) {
          return;
        }

        List<MaterialQuantity> neededMaterials = CraftingManager.getInputMaterials(recipeEntry.getValue(), 1);
        bufferMissingMaterials(containerSnapshot, neededMaterials, missingMaterials, recipeEntry.getKey());
        containerSnapshot.removeMaterials(neededMaterials, false, false, true);
        missingMealCount--;
      }
    }
  }

  private void bufferMissingMaterials(
      ItemContainer containerSnapshot,
      List<MaterialQuantity> neededMaterials,
      Map<String, Integer> missingMaterials,
      String recipeId) {
    for (MaterialQuantity material : neededMaterials) {
      String itemId = material.getItemId();
      if (itemId == null || itemId.isBlank()) {
        ProjectCivicCore.LOGGER.atSevere()
            .log("[CreateNewMealPlanAction] Cannot buffer missing material for recipe "
                + recipeId
                + " because it has no item id: "
                + material);
        continue;
      }

      int missingAmount = InternalContainerUtilMaterial.testRemoveMaterialFromItems(
          containerSnapshot,
          material,
          material.getQuantity(),
          true);

      if (missingAmount > 0) {
        missingMaterials.merge(itemId, missingAmount, Integer::sum);
      }
    }
  }

  private List<Map.Entry<String, CraftingRecipe>> getOrderedRecipes(int recipeLevel) {
    return RecipeRegistry.getLevelDependantCraftingRecipe(recipeLevel)
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .toList();
  }
}
// package
// com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.innkeeper;
//
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
//
// import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
// import com.hypixel.hytale.component.Ref;
// import com.hypixel.hytale.component.Store;
// import com.hypixel.hytale.math.vector.Vector3i;
// import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
// import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
// import
// com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
// import
// com.hypixel.hytale.server.core.inventory.container.InternalContainerUtilMaterial;
// import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
// import
// com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
// import com.hypixel.hytale.server.core.universe.world.World;
// import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
// import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
// import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
// import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
// import com.hypixel.hytale.server.npc.role.Role;
// import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
// import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
// import com.pacificbytestudios.projectciviccore.food.registry.RecipeRegistry;
// import
// com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
// import
// com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
// import
// com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest.ItemRequestCriterion;
// import
// com.pacificbytestudios.projectciviccore.village.components.workplace.tavern.TavernComponent;
// import
// com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.innkeeper.BuilderCreateNewMealPlanAction;
// import
// com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
// import
// com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
// import
// com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver;
// import
// com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver.ResolvedContainer;
//
// public class CreateNewMealPlanAction extends ActionBase {
// private final int successFlagSlot;
// private final int failureFlagSlot;
//
// public CreateNewMealPlanAction(BuilderCreateNewMealPlanAction actionBuilder,
// BuilderSupport support) {
// super(actionBuilder);
// this.successFlagSlot = actionBuilder.getSuccessFlagSlot(support);
// this.failureFlagSlot = actionBuilder.getFailureFlagSlot(support);
// }
//
// @Override
// public boolean execute(
// Ref<EntityStore> ref,
// Role role,
// InfoProvider infoProvider,
// double deltaTime,
// Store<EntityStore> store) {
// if (!super.canExecute(ref, role, infoProvider, deltaTime, store)) {
// return false;
// }
// super.execute(ref, role, infoProvider, deltaTime, store);
//
// VillagerComponent villagerComponent = store.getComponent(ref,
// VillagerComponent.getComponentType());
// if (villagerComponent == null) {
// ProjectCivicCore.LOGGER.atSevere()
// .log("[CreateNewMealPlanAction] Could not fetch villager component");
// return this.finish(role, false);
// }
//
// VillagerWorkerComponent workerComponent = villagerComponent.getWorkerData();
// if (workerComponent == null) {
// ProjectCivicCore.LOGGER.atSevere()
// .log("[CreateNewMealPlanAction] Could not fetch worker data for worker.");
// return this.finish(role, false);
// }
//
// WorkplaceComponent workplaceComponent = workerComponent.getWorkplace();
// if (workplaceComponent == null) {
// ProjectCivicCore.LOGGER.atSevere()
// .log("[CreateNewMealPlanAction] Could not fetch workplace for worker.");
// return this.finish(role, false);
// }
//
// World world = store.getExternalData().getWorld();
// Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
//
// Ref<ChunkStore> workplaceRef = workplaceComponent.getWorkplaceRef().getRef();
// if (workplaceRef == null || !workplaceRef.isValid()) {
// ProjectCivicCore.LOGGER.atSevere()
// .log("[CreateNewMealPlanAction] Invalid workplace ref");
// return this.finish(role, false);
// }
//
// TavernComponent tavernComponent = chunkStore.getComponent(workplaceRef,
// TavernComponent.getComponentType());
// if (tavernComponent == null) {
// ProjectCivicCore.LOGGER.atSevere()
// .log("[CreateNewMealPlanAction] Could not fetch the tavern component");
// return this.finish(role, false);
// }
//
// VillageHeartComponent villageHeart = chunkStore.getComponent(
// villagerComponent.getVillageHeartRef(),
// VillageHeartComponent.getComponentType());
//
// if (villageHeart == null) {
// ProjectCivicCore.LOGGER.atSevere()
// .log("[CreateNewMealPlanAction] Could not fetch village heart");
// return this.finish(role, false);
// }
//
// int currentVillageLevel = villageHeart.getLevel();
// int totalVillagers = villageHeart.getVillagerCount();
//
// if (tavernComponent.getMealToPrepareCount() >= totalVillagers) {
// ProjectCivicCore.LOGGER.atInfo()
// .log("[CreateNewMealPlanAction] Meal plan already present");
// return this.finish(role, true);
// }
//
// System.out.println("=========================================================");
// System.out.println("CREO UN NUOVO MEAL PLAN");
// System.out.println("=========================================================");
// tavernComponent.setMealToPrepare(new HashMap<>());
// tavernComponent.resetMissingResources();
//
// List<ItemContainer> storageSnapshots = new ArrayList<>();
// for (Vector3i storagePosition :
// workplaceComponent.getStorage().getAllContainerPositions()) {
// ResolvedContainer resolvedStorage =
// ContainerResolver.resolveContainerAt(world, storagePosition);
// if (resolvedStorage == null) {
// ProjectCivicCore.LOGGER.atSevere()
// .log("[CreateNewMealPlanAction] Dead reference of a container is present at "
// + storagePosition);
// return this.finish(role, false);
// }
//
// SimpleItemContainer containerSnapshot =
// resolvedStorage.containerBlock().getItemContainer().clone();
// storageSnapshots.add(containerSnapshot);
// }
//
// CombinedItemContainer combinedSnapshot = new
// CombinedItemContainer(storageSnapshots.toArray(new ItemContainer[0]));
// populateMealPlanFromSnapshot(combinedSnapshot, currentVillageLevel,
// tavernComponent, totalVillagers);
//
// int missingMealCount = totalVillagers -
// tavernComponent.getMealToPrepareCount();
// if (missingMealCount > 0) {
// Map<String, Integer> missingMaterials = new HashMap<>();
// populateMissingMaterialsForMealPlan(combinedSnapshot, currentVillageLevel,
// missingMealCount, missingMaterials);
// missingMaterials.forEach((itemId, amount) -> tavernComponent
// .addMissingResource(new ItemRequestCriterion(itemId, true, amount)));
// }
//
// System.out.println("Created meal plan: ");
// System.out.println(tavernComponent.getMealToPrepare());
// System.out.println("Meal count: ");
// System.out.println(tavernComponent.getMealToPrepareCount());
// System.out.println("Missing resources: ");
// System.out.println(tavernComponent.getMissingResources());
//
// return this.finish(role, tavernComponent.getMealToPrepareCount() >=
// totalVillagers);
// }
//
// private boolean finish(Role role, boolean success) {
// if (this.successFlagSlot >= 0) {
// role.setFlag(this.successFlagSlot, success);
// }
// if (this.failureFlagSlot >= 0) {
// role.setFlag(this.failureFlagSlot, !success);
// }
// return success;
// }
//
// private void populateMealPlanFromSnapshot(
// ItemContainer containerSnapshot,
// int recipeLevel,
// TavernComponent tavernComponent,
// int targetRecipeCount) {
// if (recipeLevel <= 0 || tavernComponent.getMealToPrepareCount() >=
// targetRecipeCount) {
// return;
// }
//
// for (Map.Entry<String, CraftingRecipe> recipeEntry :
// getOrderedRecipes(recipeLevel)) {
// if (tavernComponent.getMealToPrepareCount() >= targetRecipeCount) {
// return;
// }
//
// List<MaterialQuantity> neededMaterials =
// CraftingManager.getInputMaterials(recipeEntry.getValue(), 1);
//
// while (tavernComponent.getMealToPrepareCount() < targetRecipeCount
// && containerSnapshot.canRemoveMaterials(neededMaterials)) {
// containerSnapshot.removeMaterials(neededMaterials, true, true, true);
// tavernComponent.addMealToPrepare(recipeEntry.getKey(), 1);
// }
// }
//
// populateMealPlanFromSnapshot(
// containerSnapshot,
// recipeLevel - 1,
// tavernComponent,
// targetRecipeCount);
// }
//
// private void populateMissingMaterialsForMealPlan(
// ItemContainer containerSnapshot,
// int recipeLevel,
// int missingMealCount,
// Map<String, Integer> missingMaterials) {
// if (recipeLevel <= 0 || missingMealCount <= 0) {
// return;
// }
//
// for (Map.Entry<String, CraftingRecipe> recipeEntry :
// getOrderedRecipes(recipeLevel)) {
// List<MaterialQuantity> neededMaterials =
// CraftingManager.getInputMaterials(recipeEntry.getValue(), 1);
//
// while (missingMealCount > 0) {
// bufferMissingMaterials(containerSnapshot, neededMaterials, missingMaterials,
// recipeEntry.getKey());
// containerSnapshot.removeMaterials(neededMaterials, false, false, true);
// missingMealCount--;
// }
//
// if (missingMealCount <= 0) {
// return;
// }
// }
//
// populateMissingMaterialsForMealPlan(containerSnapshot, recipeLevel - 1,
// missingMealCount, missingMaterials);
// }
//
// private void bufferMissingMaterials(
// ItemContainer containerSnapshot,
// List<MaterialQuantity> neededMaterials,
// Map<String, Integer> missingMaterials,
// String recipeId) {
// for (MaterialQuantity material : neededMaterials) {
// String itemId = material.getItemId();
// if (itemId == null || itemId.isBlank()) {
// ProjectCivicCore.LOGGER.atSevere()
// .log("[CreateNewMealPlanAction] Cannot buffer missing material for recipe "
// + recipeId
// + " because it has no item id: "
// + material);
// continue;
// }
//
// int missingAmount =
// InternalContainerUtilMaterial.testRemoveMaterialFromItems(
// containerSnapshot,
// material,
// material.getQuantity(),
// true);
//
// if (missingAmount > 0) {
// missingMaterials.merge(itemId, missingAmount, Integer::sum);
// }
// }
// }
//
// private List<Map.Entry<String, CraftingRecipe>> getOrderedRecipes(int
// recipeLevel) {
// return RecipeRegistry.getLevelDependantCraftingRecipe(recipeLevel)
// .entrySet()
// .stream()
// .sorted(Map.Entry.comparingByKey())
// .toList();
// }
// }
// ======================================================================
// ======================================================================
// ======================================================================
// ======================================================================
// ======================================================================
// package
// com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.innkeeper;
//
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
//
// import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
// import com.hypixel.hytale.component.Ref;
// import com.hypixel.hytale.component.Store;
// import com.hypixel.hytale.math.vector.Vector3i;
// import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
// import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
// import
// com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
// import com.hypixel.hytale.server.core.universe.world.World;
// import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
// import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
// import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
// import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
// import com.hypixel.hytale.server.npc.role.Role;
// import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
// import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
// import com.pacificbytestudios.projectciviccore.food.registry.RecipeRegistry;
// import
// com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
// import
// com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
// import
// com.pacificbytestudios.projectciviccore.village.components.workplace.tavern.TavernComponent;
// import
// com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.innkeeper.BuilderCreateNewMealPlanAction;
// import
// com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
// import
// com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
// import
// com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver;
// import
// com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver.ResolvedContainer;
//
// public class CreateNewMealPlanAction extends ActionBase {
// private final int successFlagSlot;
// private final int failureFlagSlot;
//
// public CreateNewMealPlanAction(BuilderCreateNewMealPlanAction actionBuilder,
// BuilderSupport support) {
// super(actionBuilder);
// this.successFlagSlot = actionBuilder.getSuccessFlagSlot(support);
// this.failureFlagSlot = actionBuilder.getFailureFlagSlot(support);
// }
//
// @Override
// public boolean execute(
// Ref<EntityStore> ref,
// Role role,
// InfoProvider infoProvider,
// double deltaTime,
// Store<EntityStore> store) {
// if (!super.canExecute(ref, role, infoProvider, deltaTime, store)) {
// return false;
// }
// super.execute(ref, role, infoProvider, deltaTime, store);
//
// VillagerComponent villagerComponent = store.getComponent(ref,
// VillagerComponent.getComponentType());
// if (villagerComponent == null) {
// ProjectCivicCore.LOGGER.atSevere()
// .log("[CreateNewMealPlanAction] Could not fetch villager component");
// return this.finish(role, false);
// }
//
// VillagerWorkerComponent workerComponent = villagerComponent.getWorkerData();
// if (workerComponent == null) {
// ProjectCivicCore.LOGGER.atSevere()
// .log("[CreateNewMealPlanAction] Could not fetch worker data for worker.");
// return this.finish(role, false);
// }
//
// WorkplaceComponent workplaceComponent = workerComponent.getWorkplace();
// if (workplaceComponent == null) {
// ProjectCivicCore.LOGGER.atSevere()
// .log("[CreateNewMealPlanAction] Could not fetch workplace for worker.");
// return this.finish(role, false);
// }
//
// World world = store.getExternalData().getWorld();
// Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
//
// Ref<ChunkStore> workplaceRef = workplaceComponent.getWorkplaceRef().getRef();
// if (workplaceRef == null || !workplaceRef.isValid()) {
// ProjectCivicCore.LOGGER.atSevere()
// .log("[CreateNewMealPlanAction] Invalid workplace ref");
// return this.finish(role, false);
// }
//
// TavernComponent tavernComponent = chunkStore.getComponent(workplaceRef,
// TavernComponent.getComponentType());
// if (tavernComponent == null) {
// ProjectCivicCore.LOGGER.atSevere()
// .log("[CreateNewMealPlanAction] Could not fetch the tavern component");
// return this.finish(role, false);
// }
//
// VillageHeartComponent villageHeart = chunkStore.getComponent(
// villagerComponent.getVillageHeartRef(),
// VillageHeartComponent.getComponentType());
//
// if (villageHeart == null) {
// ProjectCivicCore.LOGGER.atSevere()
// .log("[CreateNewMealPlanAction] Could not fetch village heart");
// return this.finish(role, false);
// }
//
// int currentVillageLevel = villageHeart.getLevel();
// int totalVillagers = villageHeart.getVillagerCount();
//
// if (tavernComponent.getMealToPrepare().size() >= totalVillagers) {
// ProjectCivicCore.LOGGER.atInfo()
// .log("[CreateNewMealPlanAction] Meal plan already present");
// return this.finish(role, true);
// }
//
// System.out.println("=========================================================");
// System.out.println("CREO UN NUOVO MEAL PLAN");
// System.out.println("=========================================================");
// tavernComponent.setMealToPrepare(new HashMap<>());
//
// for (Vector3i storagePosition :
// workplaceComponent.getStorage().getAllContainerPositions()) {
// if (tavernComponent.getMealToPrepareCount() >= totalVillagers) {
// break;
// }
//
// ResolvedContainer resolvedStorage =
// ContainerResolver.resolveContainerAt(world, storagePosition);
// if (resolvedStorage == null) {
// ProjectCivicCore.LOGGER.atSevere()
// .log("[CreateNewMealPlanAction] Dead reference of a container is present at "
// + storagePosition);
// return this.finish(role, false);
// }
//
// SimpleItemContainer containerSnapshot =
// resolvedStorage.containerBlock().getItemContainer().clone();
// populateMealPlanFromSnapshot(containerSnapshot, currentVillageLevel,
// tavernComponent, totalVillagers);
// }
//
// System.out.println("Created meal plan: ");
// System.out.println(tavernComponent.getMealToPrepare());
// System.out.println("Meal count: ");
// System.out.println(tavernComponent.getMealToPrepareCount());
//
// return this.finish(role, tavernComponent.getMealToPrepareCount() >=
// totalVillagers);
// }
//
// private boolean finish(Role role, boolean success) {
// if (this.successFlagSlot >= 0) {
// role.setFlag(this.successFlagSlot, success);
// }
// if (this.failureFlagSlot >= 0) {
// role.setFlag(this.failureFlagSlot, !success);
// }
// return success;
// }
//
// private void populateMealPlanFromSnapshot(
// SimpleItemContainer containerSnapshot,
// int recipeLevel,
// TavernComponent tavernComponent,
// int targetRecipeCount) {
// if (recipeLevel <= 0 || tavernComponent.getMealToPrepareCount() >=
// targetRecipeCount) {
// return;
// }
//
// Map<String, CraftingRecipe> availableRecipesByLevel =
// RecipeRegistry.getLevelDependantCraftingRecipe(recipeLevel);
//
// for (Map.Entry<String, CraftingRecipe> recipeEntry :
// availableRecipesByLevel.entrySet()) {
// if (tavernComponent.getMealToPrepareCount() >= targetRecipeCount) {
// return;
// }
//
// List<MaterialQuantity> neededMaterials =
// CraftingManager.getInputMaterials(recipeEntry.getValue(), 1);
//
// while (tavernComponent.getMealToPrepareCount() < targetRecipeCount
// && containerSnapshot.canRemoveMaterials(neededMaterials)) {
// containerSnapshot.removeMaterials(neededMaterials, true, true, true);
// tavernComponent.addMealToPrepare(recipeEntry.getKey(), 1);
// }
// }
//
// populateMealPlanFromSnapshot(
// containerSnapshot,
// recipeLevel - 1,
// tavernComponent,
// targetRecipeCount);
// }
// }
