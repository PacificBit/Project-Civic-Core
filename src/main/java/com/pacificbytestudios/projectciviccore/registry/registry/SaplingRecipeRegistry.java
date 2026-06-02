package com.pacificbytestudios.projectciviccore.registry.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.pacificbytestudios.projectciviccore.registry.enums.LowSaplingQuality;
import com.pacificbytestudios.projectciviccore.registry.enums.MidSaplingQuality;
import com.pacificbytestudios.projectciviccore.registry.enums.TopSaplingQuality;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class SaplingRecipeRegistry {
  private static final Map<String, CraftingRecipe> topSaplingQualityRecipe = new ConcurrentHashMap<>();
  private static final Map<String, CraftingRecipe> midSaplingQualityRecipe = new ConcurrentHashMap<>();
  private static final Map<String, CraftingRecipe> lowSaplingQualityRecipe = new ConcurrentHashMap<>();

  private static boolean alreadyInizialized = false;

  public static void initizalize() {
    if (alreadyInizialized) {
      return;
    }
    alreadyInizialized = true;

    initizalizeCropQuality(topSaplingQualityRecipe, TopSaplingQuality.values());
    initizalizeCropQuality(midSaplingQualityRecipe, MidSaplingQuality.values());
    initizalizeCropQuality(lowSaplingQualityRecipe, LowSaplingQuality.values());
  }

  private static void initizalizeCropQuality(Map<String, CraftingRecipe> tierMap, Supplier<String>[] samplings) {
    for (Supplier<String> sapling : samplings) {
      Item item = Item.getAssetMap().getAsset(sapling.get());
      if (item == null) {
        Logger.logSevere("Could not fetch item for item id: " + sapling.get());
      }

      CraftingRecipe recipe = CraftingRecipe
          .getAssetMap()
          .getAsset(CraftingRecipe.generateIdFromItemRecipe(item, 0));

      if (recipe == null) {
        Logger.logSevere("Could not fetch recipe for item id: " + sapling.get());
        return;
      }

      tierMap.put(sapling.get(), recipe);
    }
  }

  public static CraftingRecipe getRecipe(String recipeName) {
    return lowSaplingQualityRecipe.getOrDefault(
        recipeName,
        midSaplingQualityRecipe.getOrDefault(
            recipeName,
            topSaplingQualityRecipe.get(recipeName)));
  }

  public static Map<String, CraftingRecipe> getLevelDependantSaplingRecipe(int level) {
    if (level == 1) {
      return lowSaplingQualityRecipe;
    } else if (level == 2) {
      return midSaplingQualityRecipe;
    }
    return topSaplingQualityRecipe;
  }

}
