package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.registry.enums.LowToolQuality;
import com.pacificbytestudios.projectciviccore.registry.registry.ToolRecipeRegistry;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.BuilderCraftToolAction;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class CraftToolAction extends VillagerActionBase {
  private final ToolType toolType;

  public CraftToolAction(BuilderCraftToolAction builderActionBase, BuilderSupport support) {
    super(builderActionBase);
    this.toolType = builderActionBase.getToolType(support);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      ExecutionSupport executionSupport,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.canExecute(ref, executionSupport, sensorInfo, deltaTime, store)) {
      return false;
    }
    super.execute(ref, executionSupport, sensorInfo, deltaTime, store);

    VillageHeartComponent villageHeart = this.getVillageHeartComponent(ref, store);
    ItemContainer villagerStorage = this.getVillagerStorage(ref, store);
    if (villageHeart == null || villagerStorage == null) {
      return false;
    }

    CraftingRecipe recipe = getToolRecipe(villageHeart.getLevel());
    if (recipe == null || !canApplyCraft(villagerStorage, recipe)) {
      return false;
    }

    if (!applyCraft(villagerStorage, recipe)) {
      Logger.logWarning("Planned craft failed for tool type " + this.toolType);
      return false;
    }

    Logger.logFine("Crafted tool " + this.toolType + " for village level " + villageHeart.getLevel());
    return true;
  }

  private CraftingRecipe getToolRecipe(int villageLevel) {
    Map<String, CraftingRecipe> levelRecipes = ToolRecipeRegistry.getLevelDependantCraftingRecipe(villageLevel);
    if (levelRecipes.isEmpty()) {
      Logger.logWarning("No tool recipes available for village level " + villageLevel);
      return null;
    }

    String recipeId = this.toolType.getRecipeId(villageLevel);
    CraftingRecipe recipe = levelRecipes.get(recipeId);
    if (recipe == null) {
      Logger.logWarning("No recipe found for tool type " + this.toolType + " at village level " + villageLevel
              + " using recipe id " + recipeId);
    }

    return recipe;
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
    if (outputItems.size() != 1) {
      Logger.logWarning("Expected exactly one tool output but got " + outputItems.size());
      return false;
    }

    ItemStack craftedTool = outputItems.get(0);
    if (craftedTool == null || craftedTool.isEmpty() || craftedTool.getQuantity() != 1) {
      Logger.logWarning("Expected exactly one crafted tool item but got " + craftedTool);
      return false;
    }

    if (!itemContainer.removeMaterials(inputMaterials, true, true, true).succeeded()) {
      return false;
    }

    return itemContainer.addItemStacks(outputItems, true, false, true).succeeded();
  }

  public enum ToolType implements Supplier<String> {
    HOE(LowToolQuality.HOE.get()),
    HATCHET(LowToolQuality.HATCHET.get()),
    PICKAXE(LowToolQuality.PICKAXE.get()),
    WATERING_CAN(LowToolQuality.WATERING_CAN.get());

    private final String lowQualityRecipeId;

    ToolType(String lowQualityRecipeId) {
      this.lowQualityRecipeId = lowQualityRecipeId;
    }

    public String getRecipeId(int villageLevel) {
      return this.lowQualityRecipeId;
    }

    @Override
    public String get() {
      return this.lowQualityRecipeId;
    }
  }
}
