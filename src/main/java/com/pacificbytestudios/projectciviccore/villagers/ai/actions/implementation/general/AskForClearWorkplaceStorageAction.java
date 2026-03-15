package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.ToIntFunction;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.EnqueueVillageStorageRequestTask;
import com.pacificbytestudios.projectciviccore.village.enums.VillageStorageRequestType;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderAskForClearWorkplaceStorageAction;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver.ResolvedContainer;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryItemCriteria;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryTransferValidators;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;

public class AskForClearWorkplaceStorageAction extends VillagerActionBase {

  private final String[] excludedItemTypes;

  public AskForClearWorkplaceStorageAction(
      BuilderAskForClearWorkplaceStorageAction builderActionBase,
      BuilderSupport support) {
    super(builderActionBase);
    this.excludedItemTypes = builderActionBase.getExcludedItemTypes(support);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.canExecute(ref, role, sensorInfo, deltaTime, store)) {
      return true;
    }
    super.execute(ref, role, sensorInfo, deltaTime, store);

    WorkplaceComponent workplaceComponent = this.getWorkplaceComponent(ref, store);
    Ref<ChunkStore> villageHeart = this.getVillageRef(ref, store);
    if (workplaceComponent == null || villageHeart == null) {
      return false;
    }

    if (workplaceComponent.getLastStorageRequest() != null &&
        workplaceComponent.getLastStorageRequest().getType()
            .equals(VillageStorageRequestType.CLEAR_WORKPLACE_INVENTORY)) {
      ProjectCivicCore.LOGGER.atInfo()
          .log("[AskForClearWorkplaceStorageAction] Clear storage request already present");
      return false;
    }

    World world = store.getExternalData().getWorld();
    ToIntFunction<ItemStack> validator = buildDirtyInventoryValidator();

    for (Vector3i containerPos : workplaceComponent.getStorage().getAllContainerPositions()) {
      ResolvedContainer resolvedContainer = ContainerResolver.resolveContainerAt(world, containerPos);

      if (resolvedContainer == null || resolvedContainer.containerBlock() == null) {
        continue;
      }

      ItemContainer container = resolvedContainer.containerBlock().getItemContainer();
      if (container == null) {
        continue;
      }

      if (hasDirtyInventory(container, validator)) {
        ProjectCivicCore.LOGGER.atInfo()
            .log("[AskForClearWorkplaceStorageAction] Found dirty workplace storage at "
                + containerPos);

        VillageStorageRequest request = buildClearRequest(workplaceComponent);
        ProjectCivicCore.LOGGER.atInfo()
            .log("[AskForClearWorkplaceStorageAction] Built request: " + request);

        EnqueueVillageStorageRequestTask task = new EnqueueVillageStorageRequestTask(request);
        task.setOnSuccessCallback(requestId -> {
          request.overrideRequestId(UUID.fromString(requestId));
          workplaceComponent.setLastStorageRequest(request);
        });

        VillageTaskQueueComponent taskComponent = new VillageTaskQueueComponent();
        taskComponent.registerTask(task);

        Utils.safeVillageTaskEnqueue(villageHeart, taskComponent);
        return true;
      }
    }

    return false;
  }

  private ToIntFunction<ItemStack> buildDirtyInventoryValidator() {
    if (this.excludedItemTypes == null || this.excludedItemTypes.length == 0) {
      return InventoryTransferValidators.buildTransferValidator(null, ignored -> 0);
    }

    List<InventoryItemCriteria> criteria = Arrays.stream(this.excludedItemTypes)
        .filter(itemId -> itemId != null && !itemId.isBlank())
        .map(itemId -> new InventoryItemCriteria(itemId, false, 0))
        .toList();

    if (criteria.isEmpty()) {
      return InventoryTransferValidators.buildTransferValidator(null, ignored -> 0);
    }

    return InventoryTransferValidators.buildTransferValidator(criteria, ignored -> 0);
  }

  private boolean hasDirtyInventory(
      ItemContainer container,
      ToIntFunction<ItemStack> validator) {

    for (short slot = 0; slot < container.getCapacity(); slot++) {
      ItemStack item = container.getItemStack(slot);

      if (!InventoryTransferValidators.isTransferableItem(item)) {
        continue;
      }

      if (validator.applyAsInt(item) > 0) {
        return true;
      }
    }

    return false;
  }

  private VillageStorageRequest buildClearRequest(WorkplaceComponent workplace) {

    VillageStorageRequest request = new VillageStorageRequest();
    request.setType(VillageStorageRequestType.CLEAR_WORKPLACE_INVENTORY);
    request.setRequesterWorkplaceId(workplace.getUuid());

    for (String excludeItemType : this.excludedItemTypes) {
      VillageStorageRequest.ItemRequestCriterion criteria = new VillageStorageRequest.ItemRequestCriterion();
      criteria.setFilterIn(false);
      criteria.setAttributeKey(excludeItemType);
      criteria.setRequiredAmount(-1); // all
      request.addItemCriteria(criteria);
    }

    request.setTargetsWorkplaceStorage(workplace.getStorage().getAllContainerPositions());

    return request;
  }
}
