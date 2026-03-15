package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.innkeeper;

import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.food.registry.RecipeRegistry;
import com.pacificbytestudios.projectciviccore.village.components.workplace.tavern.TavernComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;

public class CookMealAction extends VillagerActionBase {

  public CookMealAction(BuilderActionBase builderActionBase) {
    super(builderActionBase);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!canExecute(ref, role, sensorInfo, deltaTime, store)) {
      return false;
    }
    super.execute(ref, role, sensorInfo, deltaTime, store);

    Ref<ChunkStore> workplaceRef = this.getWorkplaceRef(ref, store);
    ItemContainer villagerStorage = this.getVillagerStorage(ref, store);
    if (workplaceRef == null || villagerStorage == null) {
      return false;
    }

    TavernComponent tavern = workplaceRef.getStore()
        .getComponent(workplaceRef, TavernComponent.getComponentType());
    if (tavern == null) {
      return false;
    }

    UUID workerId = store.getComponent(ref, UUIDComponent.getComponentType()).getUuid();

    String meal = tavern.getAssignedRecipe(workerId);
    if (meal == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[CookMealAction] Unexpected empty recipe to cook");
      return false;
    }

    CraftingRecipe craftingRecipe = RecipeRegistry.getRecipe(meal);
    List<MaterialQuantity> materialToRemove = CraftingManager.getInputMaterials(craftingRecipe);

    if (!villagerStorage.canRemoveMaterials(materialToRemove)) {
      ProjectCivicCore.LOGGER.atSevere().log("[CookMealAction] Cannot proceed with the cooking, missing materials");
      return false;
    }

    if (villagerStorage.removeMaterials(materialToRemove).succeeded()) {
      villagerStorage.addItemStack(new ItemStack(meal, 1));
      System.out.println("Removed items ");
      tavern.markWorkerAsFree(workerId);
      return true;
    }

    System.out.println("Could not remove item");

    return false;
  }

}
