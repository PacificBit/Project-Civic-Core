package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.farmer;

import java.util.List;
import java.util.Map;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.registry.registry.CropRecipeRegistry;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.crafting.BatchCraftingHelper;
import com.pacificbytestudios.projectciviccore.villagers.core.crafting.BatchCraftingHelper.BatchCraftingResult;
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

    BatchCraftingResult craftResult = BatchCraftingHelper.craftAll(
        villagerStorage,
        orderedRecipes,
        "[CraftSeedsAction]");
    if (!craftResult.success() || !craftResult.craftedAny()) {
      return false;
    }

    ProjectCivicCore.LOGGER.atFine()
        .log("[CraftSeedsAction] Crafted seeds: " + craftResult.craftedCounts());
    return true;
  }

  private List<Map.Entry<String, CraftingRecipe>> getOrderedRecipes(int villageLevel) {
    return CropRecipeRegistry.getLevelDependantCraftingRecipe(villageLevel)
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .toList();
  }
}
