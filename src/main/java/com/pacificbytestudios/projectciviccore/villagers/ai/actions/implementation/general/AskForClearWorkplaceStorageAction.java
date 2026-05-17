package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.ToIntFunction;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
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
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequestTracker;
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
      return false;
    }
    super.execute(ref, role, sensorInfo, deltaTime, store);

    WorkplaceComponent workplaceComponent = this.getWorkplaceComponent(ref, store);
    Ref<ChunkStore> villageHeartRef = this.getVillageRef(ref, store);
    Ref<ChunkStore> workplaceRef = this.getWorkplaceRef(ref, store);
    VillageHeartComponent villageHeart = this.getVillageHeartComponent(ref, store);
    if (workplaceComponent == null || villageHeartRef == null || workplaceRef == null || villageHeart == null) {
      return false;
    }

    boolean hasPendingClearRequest = VillageStorageRequestTracker.hasPendingRequest(
        villageHeartRef,
        villageHeart,
        workplaceComponent.getUuid(),
        VillageStorageRequestType.CLEAR_WORKPLACE_INVENTORY);

    if (hasPendingClearRequest) {
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
        task.setOnSuccessCallback(requestResult -> {
          String[] requestParts = requestResult.split(":", 2);
          UUID storageId = UUID.fromString(requestParts[0]);
          request.setStorageWorkplaceId(storageId);
          request.addTriedStorageId(storageId);
          request.overrideRequestId(UUID.fromString(requestParts[1]));
          request.setRegisteredWithStorage(true);
          syncLastStorageRequest(villageHeartRef, villageHeart, workplaceRef, workplaceComponent, request);
        });
        task.setOnError((status, message) -> syncLastStorageRequest(villageHeartRef, villageHeart, workplaceRef,
            workplaceComponent, null));

        VillageTaskQueueComponent taskComponent = new VillageTaskQueueComponent();
        taskComponent.registerTask(task);

        Utils.safeVillageTaskEnqueue(villageHeartRef, taskComponent);
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

    request.setTargetsWorkplaceStorage(workplace.getStorage().getAllContainerAccessPositions());

    return request;
  }

  private void syncLastStorageRequest(
      Ref<ChunkStore> villageHeartRef,
      VillageHeartComponent villageHeart,
      Ref<ChunkStore> workplaceRef,
      WorkplaceComponent sourceWorkplace,
      VillageStorageRequest request) {
    applyLastStorageRequest(sourceWorkplace, request);

    if (villageHeart != null && sourceWorkplace != null) {
      WorkplaceComponent registeredWorkplace = villageHeart.getWorkplaceFromId(sourceWorkplace.getUuid());
      applyLastStorageRequest(registeredWorkplace, request);
    }

    if (workplaceRef != null && workplaceRef.isValid()) {
      WorkplaceComponent persistedWorkplace = workplaceRef.getStore().getComponent(
          workplaceRef,
          WorkplaceComponent.getComponentType());
      applyLastStorageRequest(persistedWorkplace, request);
      persistWorkplaceComponent(workplaceRef, persistedWorkplace);
    }

    if (villageHeartRef != null && villageHeart != null) {
      villageHeartRef.getStore().putComponent(
          villageHeartRef,
          VillageHeartComponent.getComponentType(),
          villageHeart);
    }
  }

  private void applyLastStorageRequest(WorkplaceComponent workplace, VillageStorageRequest request) {
    if (workplace == null) {
      return;
    }

    workplace.setLastStorageRequest(request != null ? request.clone() : null);
  }

  private void persistWorkplaceComponent(Ref<ChunkStore> workplaceRef, WorkplaceComponent workplace) {
    if (workplaceRef == null || !workplaceRef.isValid() || workplace == null) {
      return;
    }

    Store<ChunkStore> chunkStore = workplaceRef.getStore();
    chunkStore.putComponent(workplaceRef, WorkplaceComponent.getComponentType(), workplace);

    BlockModule.BlockStateInfo blockStateInfo = chunkStore.getComponent(
        workplaceRef,
        BlockModule.BlockStateInfo.getComponentType());
    if (blockStateInfo != null) {
      blockStateInfo.markNeedsSaving();
    }
  }
}
