package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.innkeeper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.InternalContainerUtilMaterial;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.MoveTransaction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.registry.registry.FoodRecipeRegistry;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest.ItemRequestCriterion;
import com.pacificbytestudios.projectciviccore.village.components.workplace.tavern.TavernComponent;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.innkeeper.BuilderGatherNextMealMaterialFromContainerAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.WorkplaceStorageTargetsSnapshotAction;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver.ResolvedContainer;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.VillagerInventoryAccess;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class GatherNextMealMaterialFromContainerAction extends
    VillagerActionBase {
  private final int successFlagSlot;
  private final int failureFlagSlot;

  public GatherNextMealMaterialFromContainerAction(
      BuilderGatherNextMealMaterialFromContainerAction builderActionBase,
      BuilderSupport support) {
    super(builderActionBase);
    this.successFlagSlot = builderActionBase.getSuccessFlagSlot(support);
    this.failureFlagSlot = builderActionBase.getFailureFlagSlot(support);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      ExecutionSupport executionSupport,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!canExecute(ref, executionSupport, sensorInfo, deltaTime, store)) {
      return false;
    }
    super.execute(ref, executionSupport, sensorInfo, deltaTime, store);
    Ref<ChunkStore> workplaceRef = this.getWorkplaceRef(ref, store);
    WorkplaceAreaSnapshot snapshot = this.getAreaSnapshot(ref, store);
    WorkplaceComponent workplaceComponent = this.getWorkplaceComponent(ref, store);
    if (workplaceRef == null || snapshot == null || workplaceComponent == null) {
      return this.finish(executionSupport, false);
    }

    World world = store.getExternalData().getWorld();
    Store<ChunkStore> chunkStore = world.getChunkStore().getStore();

    TavernComponent tavernComponent = chunkStore.getComponent(workplaceRef,
        TavernComponent.getComponentType());
    if (tavernComponent == null) {
      Logger.logWarning("Could not fetch the tavern component");
      return this.finish(executionSupport, false);
    }

    tavernComponent.resetMissingResources();
    Utils.markNeedSaving(world, workplaceComponent.getBenchPos());

    UUIDComponent uuidComponent = store.getComponent(ref,
        UUIDComponent.getComponentType());
    String mealToPrepare = tavernComponent.getAssignedRecipe(uuidComponent.getUuid());

    if (mealToPrepare == null) {
      Logger.logWarning("Got cook request but got no meal to prepare assigned");
      return this.finish(executionSupport, false);
    }

    Vec3i containerCoords = snapshot.pollNextSnapshotTargetBlockFrom(
        WorkplaceStorageTargetsSnapshotAction.WORKPLACE_STORAGE_KEY);

    CraftingRecipe recipe = FoodRecipeRegistry.getRecipe(mealToPrepare);
    List<MaterialQuantity> neededMaterials = CraftingManager.getInputMaterials(recipe, 1);

    ResolvedContainer container = ContainerResolver.resolveContainerAt(world,
        containerCoords);
    if (container == null) {
      Logger.logWarning("No workplace container found in designated coords");
      return this.finish(executionSupport, false);
    }

    SimpleItemContainer workplaceStorage = container.containerBlock().getItemContainer();

    ItemContainer npcStorage = VillagerInventoryAccess.storage(store, ref);
    if (npcStorage == null) {
      Logger.logWarning("Could not find villager storage");
      return this.finish(executionSupport, false);
    }

    Map<String, Integer> missingMaterials = new HashMap<>();

    for (MaterialQuantity material : neededMaterials) {
      int missing = InternalContainerUtilMaterial.testRemoveMaterialFromItems(
          npcStorage,
          material,
          material.getQuantity(),
          true);
      if (missing <= 0) {
        continue;
      }

      ItemStack itemStack = material.toItemStack();

      for (short slot = 0; slot < workplaceStorage.getCapacity() && missing > 0; slot++) {
        ItemStack stack = workplaceStorage.getItemStack(slot);
        if (stack == null || stack.isEmpty()) {
          continue;
        }

        boolean matches = itemStack != null
            ? itemStack.isEquivalentType(stack)
            : CraftingManager.matches(material, stack);

        if (!matches) {
          continue;
        }

        int quantityToMove = Math.min(stack.getQuantity(), missing);

        MoveTransaction<ItemStackTransaction> move = workplaceStorage.moveItemStackFromSlot(
            slot,
            quantityToMove,
            npcStorage,
            false,
            true);
        if (move == null || !move.succeeded()) {
          Logger.logWarning("Could not move "
              + quantityToMove
              + " of "
              + stack.getItemId()
              + " from workplace storage to NPC inventory");
          continue;
        }

        ItemStack remainder = move.getAddTransaction() != null
            ? move.getAddTransaction().getRemainder()
            : null;

        int notMoved = remainder != null ? remainder.getQuantity() : 0;
        int moved = quantityToMove - notMoved;

        missing -= moved;
      }

      if (missing > 0) {
        String itemId = material.getItemId();
        if (itemId == null || itemId.isBlank()) {
          Logger.logWarning("Cannot buffer missing material without item id: "
              + material);
          continue;
        }
        missingMaterials.merge(itemId, missing, Integer::sum);
      }
    }

    if (!missingMaterials.isEmpty()) {
      missingMaterials.forEach((itemId, amount) -> tavernComponent
          .addMissingResource(new ItemRequestCriterion(itemId, true, amount)));
      Logger.logFine("Missing materials: " +
          missingMaterials);
      return this.finish(executionSupport, false);
    }

    return this.finish(executionSupport, true);
  }

  private boolean finish(ExecutionSupport executionSupport, boolean success) {
    if (this.successFlagSlot >= 0) {
      executionSupport.getFlagsComponent().setFlag(this.successFlagSlot, success);
    }
    if (this.failureFlagSlot >= 0) {
      executionSupport.getFlagsComponent().setFlag(this.failureFlagSlot, !success);
    }
    return success;
  }
}
