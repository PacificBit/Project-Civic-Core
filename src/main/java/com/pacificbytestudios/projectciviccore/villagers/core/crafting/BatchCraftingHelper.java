package com.pacificbytestudios.projectciviccore.villagers.core.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public final class BatchCraftingHelper {
  private BatchCraftingHelper() {
  }

  public static BatchCraftingResult craftAll(
      ItemContainer itemContainer,
      List<Map.Entry<String, CraftingRecipe>> orderedRecipes,
      String logPrefix) {
    ItemContainer storageSnapshot = itemContainer.clone();
    List<Map.Entry<String, CraftingRecipe>> craftSequence = buildCraftSequence(storageSnapshot, orderedRecipes,
        logPrefix);
    if (craftSequence.isEmpty()) {
      return new BatchCraftingResult(true, false, Map.of());
    }

    Map<String, Integer> craftedItems = new LinkedHashMap<>();
    for (Map.Entry<String, CraftingRecipe> recipeEntry : craftSequence) {
      if (!applyCraft(itemContainer, recipeEntry.getValue())) {
        ProjectCivicCore.LOGGER.atSevere()
            .log(logPrefix + " Planned craft failed for recipe " + recipeEntry.getKey());
        return new BatchCraftingResult(false, !craftedItems.isEmpty(), craftedItems);
      }

      craftedItems.merge(recipeEntry.getKey(), 1, Integer::sum);
    }

    return new BatchCraftingResult(true, true, craftedItems);
  }

  public static boolean canCraft(ItemContainer itemContainer, CraftingRecipe recipe) {
    if (itemContainer == null || recipe == null) {
      return false;
    }

    return canApplyCraft(itemContainer, recipe);
  }

  public static boolean canCraftAny(
      ItemContainer itemContainer,
      List<Map.Entry<String, CraftingRecipe>> orderedRecipes) {
    if (itemContainer == null || orderedRecipes == null || orderedRecipes.isEmpty()) {
      return false;
    }

    for (Map.Entry<String, CraftingRecipe> recipeEntry : orderedRecipes) {
      if (recipeEntry != null && canCraft(itemContainer, recipeEntry.getValue())) {
        return true;
      }
    }

    return false;
  }

  private static List<Map.Entry<String, CraftingRecipe>> buildCraftSequence(
      ItemContainer storageSnapshot,
      List<Map.Entry<String, CraftingRecipe>> orderedRecipes,
      String logPrefix) {
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
              .log(logPrefix + " Snapshot craft failed unexpectedly for recipe " + recipeEntry.getKey());
          continue;
        }

        craftSequence.add(recipeEntry);
        craftedAnyRecipeInRound = true;
      }
    } while (craftedAnyRecipeInRound);

    return craftSequence;
  }

  private static boolean canApplyCraft(ItemContainer itemContainer, CraftingRecipe recipe) {
    return applyCraft(itemContainer.clone(), recipe);
  }

  private static boolean applyCraft(ItemContainer itemContainer, CraftingRecipe recipe) {
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

  public record BatchCraftingResult(
      boolean success,
      boolean craftedAny,
      Map<String, Integer> craftedCounts) {
    public BatchCraftingResult {
      craftedCounts = Collections.unmodifiableMap(new LinkedHashMap<>(craftedCounts));
    }
  }
}
