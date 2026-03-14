package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.innkeeper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.MoveTransaction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.food.registry.RecipeRegistry;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.tavern.TavernComponent;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.WorkplaceStorageTargetsSnapshotAction;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver.ResolvedContainer;

public class GatherNextMealMaterialFromContainerAction extends ActionBase {

  public GatherNextMealMaterialFromContainerAction(BuilderActionBase builderActionBase) {
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

    VillagerComponent villagerComponent = store.getComponent(ref, VillagerComponent.getComponentType());
    if (villagerComponent == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[GatherNextMealMaterialFromContainerAction] Could not fetch villager component");
      return false;
    }

    VillagerWorkerComponent workerComponent = villagerComponent.getWorkerData();
    if (workerComponent == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[GatherNextMealMaterialFromContainerAction] Could not fetch worker data for worker.");
      return false;
    }

    WorkplaceComponent workplaceComponent = workerComponent.getWorkplace();
    if (workplaceComponent == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[GatherNextMealMaterialFromContainerAction] Could not fetch workplace for worker.");
      return false;
    }

    VillagerWorkerComponent workData = villagerComponent.getWorkerData();
    if (workData == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[GatherNextMealMaterialFromContainerAction] Could not fetch worker data.");
      return false;
    }

    WorkplaceAreaSnapshot snapshot = workData.getWorkareaSnapshot();
    if (snapshot == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[GatherNextMealMaterialFromContainerAction] Could not fetch worker area snapshot.");
      return false;
    }

    World world = store.getExternalData().getWorld();
    Store<ChunkStore> chunkStore = world.getChunkStore().getStore();

    Ref<ChunkStore> workplaceRef = workplaceComponent.getWorkplaceRef().getRef();
    if (workplaceRef == null || !workplaceRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[GatherNextMealMaterialFromContainerAction] Invalid workplace ref");
      return false;
    }

    TavernComponent tavernComponent = chunkStore.getComponent(workplaceRef, TavernComponent.getComponentType());
    if (tavernComponent == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[GatherNextMealMaterialFromContainerAction] Could not fetch the tavern component");
      return false;
    }

    UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
    String mealToPrepare = tavernComponent.getAssignedRecipe(uuidComponent.getUuid());

    if (mealToPrepare == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[GatherNextMealMaterialFromContainerAction] Got cook request but got no meal to prepare assigned");
      return false;
    }

    System.out.println("=========================================================");
    System.out.println(
        "Gathering resources for next meal: " + mealToPrepare + ", action performed by: " + uuidComponent.getUuid());
    System.out
        .println(snapshot.getBlockTypePosition().get(WorkplaceStorageTargetsSnapshotAction.WORKPLACE_STORAGE_KEY));
    System.out.println(tavernComponent.getMealToPrepare());
    System.out.println("=========================================================");

    Vector3i containerCoords = snapshot.getBlockTypePosition()
        .get(WorkplaceStorageTargetsSnapshotAction.WORKPLACE_STORAGE_KEY).pollFirst();
    if (containerCoords == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[GatherNextMealMaterialFromContainerAction] No container coords found");
      return false;
    }

    CraftingRecipe recipe = RecipeRegistry.getRecipe(mealToPrepare);
    List<MaterialQuantity> neededMaterials = CraftingManager.getInputMaterials(recipe, 1);

    ResolvedContainer container = ContainerResolver.resolveContainerAt(world, containerCoords);
    if (container == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[GatherNextMealMaterialFromContainerAction] No workplace container found in designated coords");
      return false;
    }

    SimpleItemContainer workplaceStorage = container.containerBlock().getItemContainer();

    NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
    ItemContainer npcStorage = npcEntity.getInventory().getStorage();

    Map<String, Integer> missingMaterials = new HashMap<>();

    for (MaterialQuantity material : neededMaterials) {
      int missing = material.getQuantity();
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
          ProjectCivicCore.LOGGER.atWarning()
              .log("[GatherNextMealMaterialFromContainerAction] Could not move "
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
          ProjectCivicCore.LOGGER.atSevere()
              .log("[GatherNextMealMaterialFromContainerAction] Cannot buffer missing material without item id: "
                  + material);
          continue;
        }
        missingMaterials.merge(itemId, missing, Integer::sum);
      }
    }

    if (!missingMaterials.isEmpty()) {
      ProjectCivicCore.LOGGER.atFine()
          .log("[GatherNextMealMaterialFromContainerAction] Missing materials: " + missingMaterials);
      return false;
    }

    tavernComponent.markWorkerAsFree(uuidComponent.getUuid());
    System.out.println("Gather successfull");
    return true;
  }
}
