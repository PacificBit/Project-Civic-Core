package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.innkeeper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.ItemStack;
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
  private final int mealBuffer;

  public CreateNewMealPlanAction(BuilderCreateNewMealPlanAction actionBuilder, BuilderSupport support) {
    super(actionBuilder);
    this.successFlagSlot = actionBuilder.getSuccessFlagSlot(support);
    this.failureFlagSlot = actionBuilder.getFailureFlagSlot(support);
    this.mealBuffer = actionBuilder.getMealBuffer(support);
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
    int targetMealCount = Math.max(0, villageHeart.getVillagerCount() + this.mealBuffer);

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

    int readyMealsInStorage = countReadyMealsInStorage(storageSnapshots);
    int committedMealCount = readyMealsInStorage
        + tavernComponent.getMealToPrepareCount()
        + tavernComponent.getAssignedRecipeCount();

    if (committedMealCount >= targetMealCount) {
      tavernComponent.resetMissingResources();
      ProjectCivicCore.LOGGER.atInfo()
          .log("[CreateNewMealPlanAction] Workplace food target already satisfied: "
              + committedMealCount
              + "/"
              + targetMealCount);
      return this.finish(role, true);
    }

    tavernComponent.setMealToPrepare(new HashMap<>());
    tavernComponent.resetMissingResources();

    int targetPlannedMeals = Math.max(0,
        targetMealCount - readyMealsInStorage - tavernComponent.getAssignedRecipeCount());

    CombinedItemContainer combinedSnapshot = new CombinedItemContainer(storageSnapshots.toArray(new ItemContainer[0]));
    populateMealPlanFromSnapshot(combinedSnapshot, currentVillageLevel, tavernComponent, targetPlannedMeals);

    int missingMealCount = targetPlannedMeals - tavernComponent.getMealToPrepareCount();
    if (missingMealCount > 0) {
      Map<String, Integer> missingMaterials = new HashMap<>();
      populateMissingMaterialsForMealPlan(combinedSnapshot, currentVillageLevel, missingMealCount, missingMaterials);
      missingMaterials.forEach((itemId, amount) -> tavernComponent
          .addMissingResource(new ItemRequestCriterion(itemId, true, amount)));
    }

    int totalCommittedMeals = readyMealsInStorage
        + tavernComponent.getMealToPrepareCount()
        + tavernComponent.getAssignedRecipeCount();
    return this.finish(role, totalCommittedMeals >= targetMealCount);
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

  private int countReadyMealsInStorage(List<ItemContainer> storageSnapshots) {
    Set<String> mealItemIds = new HashSet<>();
    mealItemIds.addAll(RecipeRegistry.getLowFoodQualityRecipe().keySet());
    mealItemIds.addAll(RecipeRegistry.getMidFoodQualityRecipe().keySet());
    mealItemIds.addAll(RecipeRegistry.getTopFoodQualityRecipe().keySet());

    int readyMeals = 0;
    for (ItemContainer storageSnapshot : storageSnapshots) {
      for (short slot = 0; slot < storageSnapshot.getCapacity(); slot++) {
        ItemStack stack = storageSnapshot.getItemStack(slot);
        if (stack == null || stack.isEmpty()) {
          continue;
        }
        if (mealItemIds.contains(stack.getItemId())) {
          readyMeals += stack.getQuantity();
        }
      }
    }

    return readyMeals;
  }
}
