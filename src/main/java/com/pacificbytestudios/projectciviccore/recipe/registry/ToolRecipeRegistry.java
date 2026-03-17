package com.pacificbytestudios.projectciviccore.recipe.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.recipe.enums.LowToolQuality;

public class ToolRecipeRegistry {
  private static final Map<String, CraftingRecipe> lowToolQualityRecipe = new ConcurrentHashMap<>();

  private static boolean alreadyInizialized = false;

  public static void initizalize() {
    if (alreadyInizialized) {
      return;
    }
    alreadyInizialized = true;

    initizalizeCropQuality(lowToolQualityRecipe, LowToolQuality.values());
  }

  private static void initizalizeCropQuality(Map<String, CraftingRecipe> tierMap, Supplier<String>[] foods) {
    for (Supplier<String> food : foods) {
      Item item = Item.getAssetMap().getAsset(food.get());
      if (item == null) {
        ProjectCivicCore.LOGGER.atSevere().log("[ToolRecipeRegistry] Could not fetch item for item id: " + food.get());
      }

      CraftingRecipe recipe = CraftingRecipe
          .getAssetMap()
          .getAsset(CraftingRecipe.generateIdFromItemRecipe(item, 0));

      if (recipe == null) {
        ProjectCivicCore.LOGGER.atSevere()
            .log("[ToolRecipeRegistry] Could not fetch recipe for item id: " + food.get());
        return;
      }

      tierMap.put(food.get(), recipe);
    }
  }

  public static CraftingRecipe getRecipe(String recipeName) {
    return lowToolQualityRecipe.get(recipeName);
  }

  public static Map<String, CraftingRecipe> getLowFoodQualityRecipe() {
    return lowToolQualityRecipe;
  }

  public static Map<String, CraftingRecipe> getLevelDependantCraftingRecipe(int level) {
    return getLowFoodQualityRecipe();
  }
}
