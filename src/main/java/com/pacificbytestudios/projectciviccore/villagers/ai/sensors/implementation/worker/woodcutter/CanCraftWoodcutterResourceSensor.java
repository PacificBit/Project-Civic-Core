package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.woodcutter;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.registry.registry.SaplingRecipeRegistry;
import com.pacificbytestudios.projectciviccore.registry.registry.ToolRecipeRegistry;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.CraftToolAction.ToolType;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.woodcutter.BuilderCanCraftWoodcutterResourceSensor;
import com.pacificbytestudios.projectciviccore.villagers.core.crafting.BatchCraftingHelper;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class CanCraftWoodcutterResourceSensor extends VillagerSensorBase {
  private final ResourceType resourceType;

  public CanCraftWoodcutterResourceSensor(
      BuilderCanCraftWoodcutterResourceSensor builderSensorBase,
      BuilderSupport support) {
    super(builderSensorBase);
    this.resourceType = builderSensorBase.getResourceType(support);
  }

  @Override
  public InfoProvider getSensorInfo() {
    return null;
  }

  @Override
  public boolean matches(
      Ref<EntityStore> ref,
      ExecutionSupport executionSupport,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.matches(ref, executionSupport, deltaTime, store)) {
      return false;
    }

    VillageHeartComponent villageHeart = this.getVillageHeartComponent(ref, store);
    ItemContainer villagerStorage = this.getVillagerStorage(ref, store);
    if (villageHeart == null || villagerStorage == null) {
      return false;
    }

    return switch (this.resourceType) {
      case SAPLING -> canCraftSaplings(villagerStorage, villageHeart.getLevel());
      case HATCHET -> canCraftHatchet(villagerStorage, villageHeart.getLevel());
    };
  }

  private boolean canCraftSaplings(ItemContainer villagerStorage, int villageLevel) {
    List<Map.Entry<String, CraftingRecipe>> orderedRecipes = SaplingRecipeRegistry.getLevelDependantSaplingRecipe(
        villageLevel)
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .toList();

    if (orderedRecipes.isEmpty()) {
      Logger.logWarning("No sapling recipes available for village level " + villageLevel);
      return false;
    }

    return BatchCraftingHelper.canCraftAny(villagerStorage, orderedRecipes);
  }

  private boolean canCraftHatchet(ItemContainer villagerStorage, int villageLevel) {
    Map<String, CraftingRecipe> levelRecipes = ToolRecipeRegistry.getLevelDependantCraftingRecipe(villageLevel);
    if (levelRecipes.isEmpty()) {
      Logger.logWarning("No tool recipes available for village level " + villageLevel);
      return false;
    }

    String recipeId = ToolType.HATCHET.getRecipeId(villageLevel);
    CraftingRecipe recipe = levelRecipes.get(recipeId);
    if (recipe == null) {
      Logger.logWarning("No hatchet recipe found for village level " + villageLevel
              + " using recipe id " + recipeId);
      return false;
    }

    return BatchCraftingHelper.canCraft(villagerStorage, recipe);
  }

  public enum ResourceType implements Supplier<String> {
    SAPLING("sapling"),
    HATCHET("hatchet");

    private final String type;

    ResourceType(String type) {
      this.type = type;
    }

    @Override
    public String get() {
      return this.type;
    }
  }
}
