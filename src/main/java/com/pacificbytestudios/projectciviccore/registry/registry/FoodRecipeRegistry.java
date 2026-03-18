package com.pacificbytestudios.projectciviccore.registry.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.registry.enums.LowFoodQuality;
import com.pacificbytestudios.projectciviccore.registry.enums.MidFoodQuality;
import com.pacificbytestudios.projectciviccore.registry.enums.TopFoodQuality;

public class FoodRecipeRegistry {
  private static final Map<String, CraftingRecipe> topFoodQualityRecipe = new ConcurrentHashMap<>();
  private static final Map<String, CraftingRecipe> midFoodQualityRecipe = new ConcurrentHashMap<>();
  private static final Map<String, CraftingRecipe> lowFoodQualityRecipe = new ConcurrentHashMap<>();

  private static boolean alreadyInizialized = false;

  public static void initizalize() {
    if (alreadyInizialized) {
      return;
    }
    alreadyInizialized = true;

    initizalizeFoodQuality(topFoodQualityRecipe, TopFoodQuality.values());
    initizalizeFoodQuality(midFoodQualityRecipe, MidFoodQuality.values());
    initizalizeFoodQuality(lowFoodQualityRecipe, LowFoodQuality.values());
  }

  private static void initizalizeFoodQuality(Map<String, CraftingRecipe> tierMap, Supplier<String>[] foods) {
    for (Supplier<String> food : foods) {
      Item item = Item.getAssetMap().getAsset(food.get());
      if (item == null) {
        ProjectCivicCore.LOGGER.atSevere().log("[FoodRecipeRegistry] Could not fetch item for item id: " + food.get());
      }

      CraftingRecipe recipe = CraftingRecipe
          .getAssetMap()
          .getAsset(CraftingRecipe.generateIdFromItemRecipe(item, 0));

      if (recipe == null) {
        ProjectCivicCore.LOGGER.atSevere()
            .log("[FoodRecipeRegistry] Could not fetch recipe for item id: " + food.get());
        return;
      }

      tierMap.put(food.get(), recipe);
    }
  }

  public static CraftingRecipe getRecipe(String recipeName) {
    return lowFoodQualityRecipe.getOrDefault(
        recipeName,
        midFoodQualityRecipe.getOrDefault(
            recipeName,
            topFoodQualityRecipe.get(recipeName)));
  }

  public static Map<String, CraftingRecipe> getTopFoodQualityRecipe() {
    return topFoodQualityRecipe;
  }

  public static Map<String, CraftingRecipe> getMidFoodQualityRecipe() {
    return midFoodQualityRecipe;
  }

  public static Map<String, CraftingRecipe> getLowFoodQualityRecipe() {
    return lowFoodQualityRecipe;
  }

  public static Map<String, CraftingRecipe> getLevelDependantCraftingRecipe(int level) {
    if (level == 1) {
      return getLowFoodQualityRecipe();
    } else if (level == 2) {
      return getMidFoodQualityRecipe();
    }
    return getTopFoodQualityRecipe();
  }
}
