package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.farmer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.registry.registry.CropRecipeRegistry;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;

public class CraftSeedsAction extends VillagerActionBase {

  public CraftSeedsAction(BuilderActionBase builderActionBase) {
    super(builderActionBase);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.canExecute(ref, role, sensorInfo, deltaTime, store)) {
      return false;
    }
    super.execute(ref, role, sensorInfo, deltaTime, store);

    VillageHeartComponent villageHeart = this.getVillageHeartComponent(ref, store);
    ItemContainer villagerStorage = this.getVillagerStorage(ref, store);
    if (villageHeart == null || villagerStorage == null) {
      return false;
    }

    List<Map.Entry<String, CraftingRecipe>> orderedRecipes = getOrderedRecipes(villageHeart.getLevel());
    if (orderedRecipes.isEmpty()) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[CraftSeedsAction] No crop recipes available for village level " + villageHeart.getLevel());
      return false;
    }

    ItemContainer storageSnapshot = villagerStorage.clone();
    List<Map.Entry<String, CraftingRecipe>> craftSequence = buildCraftSequence(storageSnapshot, orderedRecipes);
    if (craftSequence.isEmpty()) {
      return false;
    }

    Map<String, Integer> craftedSeeds = new LinkedHashMap<>();
    for (Map.Entry<String, CraftingRecipe> recipeEntry : craftSequence) {
      if (!applyCraft(villagerStorage, recipeEntry.getValue())) {
        ProjectCivicCore.LOGGER.atSevere()
            .log("[CraftSeedsAction] Planned craft failed for recipe " + recipeEntry.getKey());
        return false;
      }
      craftedSeeds.merge(recipeEntry.getKey(), 1, Integer::sum);
    }

    ProjectCivicCore.LOGGER.atFine()
        .log("[CraftSeedsAction] Crafted seeds: " + craftedSeeds);
    return true;
  }

  private List<Map.Entry<String, CraftingRecipe>> buildCraftSequence(
      ItemContainer storageSnapshot,
      List<Map.Entry<String, CraftingRecipe>> orderedRecipes) {
    List<Map.Entry<String, CraftingRecipe>> craftSequence = new ArrayList<>();
    boolean craftedAnyRecipeInRound;

    do {
      craftedAnyRecipeInRound = false;

      for (Map.Entry<String, CraftingRecipe> recipeEntry : orderedRecipes) {
        if (!canApplyCraft(storageSnapshot, recipeEntry.getValue())) {
          continue;
        }
        if (!applyCraft(storageSnapshot, recipeEntry.getValue())) {
          ProjectCivicCore.LOGGER.atSevere()
              .log("[CraftSeedsAction] Snapshot craft failed unexpectedly for recipe " + recipeEntry.getKey());
          continue;
        }

        craftSequence.add(recipeEntry);
        craftedAnyRecipeInRound = true;
      }
    } while (craftedAnyRecipeInRound);

    return craftSequence;
  }

  private boolean canApplyCraft(ItemContainer itemContainer, CraftingRecipe recipe) {
    return applyCraft(itemContainer.clone(), recipe);
  }

  private boolean applyCraft(ItemContainer itemContainer, CraftingRecipe recipe) {
    List<MaterialQuantity> inputMaterials = CraftingManager.getInputMaterials(recipe, 1);
    if (!itemContainer.canRemoveMaterials(inputMaterials)) {
      return false;
    }

    List<ItemStack> outputItems = CraftingManager.getOutputItemStacks(recipe, 1);
    if (outputItems.isEmpty()) {
      return false;
    }

    if (!itemContainer.removeMaterials(inputMaterials, true, true, true).succeeded()) {
      return false;
    }

    return itemContainer.addItemStacks(outputItems, true, false, true).succeeded();
  }

  private List<Map.Entry<String, CraftingRecipe>> getOrderedRecipes(int villageLevel) {
    return CropRecipeRegistry.getLevelDependantCraftingRecipe(villageLevel)
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .toList();
  }
}
