package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.storagekeeper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
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
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest.ItemRequestCriterion;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.WorkplaceVillageStorageRequestComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.EnqueueVillageStorageRequestTask;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageStorageRequestType;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.storagekeeper.BuilderUpdateRequestStatusAction;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryItemCriteria;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryTransferValidators;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;

public class UpdateRequestStatusAction extends VillagerActionBase {

  private final StatusType status;

  public UpdateRequestStatusAction(
      BuilderUpdateRequestStatusAction builderActionBase,
      BuilderSupport support) {
    super(builderActionBase);
    this.status = builderActionBase.getStatusType(support);
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

    Ref<ChunkStore> workplaceRef = this.getWorkplaceRef(ref, store);
    Ref<ChunkStore> villageHeartRef = this.getVillageRef(ref, store);
    if (workplaceRef == null || villageHeartRef == null) {
      return false;
    }

    UUID workerId = store.getComponent(ref, UUIDComponent.getComponentType()).getUuid();
    Store<ChunkStore> chunkStore = workplaceRef.getStore();
    WorkplaceVillageStorageRequestComponent storage = chunkStore.getComponent(
        workplaceRef,
        WorkplaceVillageStorageRequestComponent.getComponentType());

    if (storage == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[UpdateRequestStatusAction] Could not fetch workplace storage request component for workplace ref "
              + workplaceRef);
      return false;
    }

    ArrayDeque<VillageStorageRequest> assignedRequests = new ArrayDeque<>(storage.getAssignedRequest(workerId));

    boolean shouldFinalizeRequest = this.status.equals(StatusType.COMPLETED)
        || this.status.equals(StatusType.REJECTED);
    if (!shouldFinalizeRequest) {
      return storage.unassignRequest(workerId);
    }

    boolean removed = storage.removeCompletedRequest(workerId);
    if (removed) {
      updateFinalizedRequesterState(villageHeartRef, assignedRequests);
      persistStorageRequestComponent(chunkStore, workplaceRef, storage);
    }
    return removed;
  }

  public enum StatusType implements Supplier<String> {
    COMPLETED("completed"),
    REJECTED("rejected");

    private final String type;

    StatusType(String type) {
      this.type = type;
    }

    @Override
    public String get() {
      return this.type;
    }
  }

  private void updateFinalizedRequesterState(
      Ref<ChunkStore> villageHeartRef,
      Iterable<VillageStorageRequest> assignedRequests) {
    for (VillageStorageRequest request : assignedRequests) {
      if (request == null) {
        continue;
      }

      VillageStorageRequest retryRequest = buildRetryRequest(villageHeartRef, request);
      if (retryRequest == null) {
        syncRequesterStorageRequest(villageHeartRef, request, null);
        continue;
      }

      if (shouldLetRequesterRetry(villageHeartRef, request)) {
        prepareRequesterRetryRequest(villageHeartRef, request, retryRequest);
        continue;
      }

      queueRetryRequest(villageHeartRef, request, retryRequest);
    }
  }

  private VillageStorageRequest buildRetryRequest(
      Ref<ChunkStore> villageHeartRef,
      VillageStorageRequest request) {
    if (request == null || request.getRequesterWorkplaceId() == null) {
      return null;
    }

    if (this.status.equals(StatusType.COMPLETED)) {
      if (!VillageStorageRequestType.DELIVER_TO_WORKPLACE.equals(request.getType())) {
        return null;
      }

      VillageStorageRequest retryRequest = buildMissingDeliverRequest(villageHeartRef, request);
      if (retryRequest != null) {
        retryRequest.setRegisteredWithStorage(false);
      }
      return retryRequest;
    }

    if (!this.status.equals(StatusType.REJECTED)) {
      return null;
    }

    if (VillageStorageRequestType.DELIVER_TO_WORKPLACE.equals(request.getType())) {
      VillageStorageRequest retryRequest = buildMissingDeliverRequest(villageHeartRef, request);
      if (retryRequest != null) {
        retryRequest.setRegisteredWithStorage(false);
      }
      return retryRequest;
    }

    VillageStorageRequest retryRequest = request.clone();
    retryRequest.setRegisteredWithStorage(false);
    return retryRequest;
  }

  private VillageStorageRequest buildMissingDeliverRequest(
      Ref<ChunkStore> villageHeartRef,
      VillageStorageRequest request) {
    List<InventoryItemCriteria> missingCriteria = resolveMissingDeliverCriteria(villageHeartRef, request);
    if (missingCriteria.isEmpty()) {
      return null;
    }

    VillageStorageRequest retryRequest = request.clone();
    HashSet<ItemRequestCriterion> retriedCriteria = new HashSet<>();
    for (InventoryItemCriteria criteria : missingCriteria) {
      retriedCriteria.add(criteria.toVillageRequestCriterion());
    }

    retryRequest.setItemCriteria(retriedCriteria);
    return retryRequest;
  }

  private List<InventoryItemCriteria> resolveMissingDeliverCriteria(
      Ref<ChunkStore> villageHeartRef,
      VillageStorageRequest request) {
    if (request == null || request.getItemCriteria() == null || request.getItemCriteria().isEmpty()) {
      return List.of();
    }

    List<InventoryItemCriteria> requestedCriteria = request.getItemCriteria()
        .stream()
        .map(ItemRequestCriterion::toCriteria)
        .toList();
    if (requestedCriteria.isEmpty()) {
      return List.of();
    }

    if (villageHeartRef == null || !villageHeartRef.isValid()) {
      return requestedCriteria;
    }

    World world = villageHeartRef.getStore().getExternalData().getWorld();
    if (world == null) {
      return requestedCriteria;
    }

    List<ItemContainer> targetContainers = resolveTargetContainers(world, request.getTargetsWorkplaceStorage());
    if (targetContainers.isEmpty()) {
      return requestedCriteria;
    }

    return InventoryTransferValidators.getMissingCriteria(targetContainers, requestedCriteria);
  }

  private List<ItemContainer> resolveTargetContainers(
      World world,
      Iterable<com.hypixel.hytale.math.vector.Vector3i> targetPositions) {
    List<ItemContainer> containers = new ArrayList<>();
    HashSet<String> resolvedContainerKeys = new HashSet<>();

    if (world == null || targetPositions == null) {
      return containers;
    }

    for (com.hypixel.hytale.math.vector.Vector3i targetPosition : targetPositions) {
      ContainerResolver.ResolvedContainer resolved = ContainerResolver.resolveContainerAt(world, targetPosition);
      if (resolved == null || resolved.containerBlock() == null || resolved.basePosition() == null) {
        continue;
      }

      String containerKey = resolved.basePosition().x + ":" + resolved.basePosition().y + ":"
          + resolved.basePosition().z;
      if (!resolvedContainerKeys.add(containerKey)) {
        continue;
      }

      ItemContainer container = resolved.containerBlock().getItemContainer();
      if (container != null) {
        containers.add(container);
      }
    }

    return containers;
  }

  private void queueRetryRequest(
      Ref<ChunkStore> villageHeartRef,
      VillageStorageRequest previousRequest,
      VillageStorageRequest retryRequest) {
    if (previousRequest == null || retryRequest == null) {
      return;
    }

    UUID previousRequestId = previousRequest.getRequestId();
    retryRequest.setRegisteredWithStorage(false);
    retryRequest.setStorageWorkplaceId(null);
    retryRequest.addTriedStorageId(previousRequest.getStorageWorkplaceId());

    syncRequesterStorageRequest(
        villageHeartRef,
        previousRequestId,
        retryRequest.getRequesterWorkplaceId(),
        null);

    EnqueueVillageStorageRequestTask task = new EnqueueVillageStorageRequestTask(
        retryRequest,
        retryRequest.getTriedStorageIds());
    task.setOnSuccessCallback(requestResult -> {
      String[] requestParts = requestResult.split(":", 2);
      UUID storageId = UUID.fromString(requestParts[0]);
      retryRequest.setStorageWorkplaceId(storageId);
      retryRequest.addTriedStorageId(storageId);
      retryRequest.overrideRequestId(UUID.fromString(requestParts[1]));
      retryRequest.setRegisteredWithStorage(true);
      syncRequesterStorageRequest(villageHeartRef, previousRequestId, retryRequest.getRequesterWorkplaceId(),
          retryRequest);
    });
    task.setOnError((status, message) -> {
      retryRequest.setRegisteredWithStorage(false);
      retryRequest.setStorageWorkplaceId(null);
      retryRequest.clearTriedStorageIds();
      syncRequesterStorageRequest(villageHeartRef, previousRequestId, retryRequest.getRequesterWorkplaceId(),
          retryRequest);
    });

    VillageTaskQueueComponent taskComponent = new VillageTaskQueueComponent();
    taskComponent.registerTask(task);
    Utils.safeVillageTaskEnqueue(villageHeartRef, taskComponent);
  }

  private boolean shouldLetRequesterRetry(
      Ref<ChunkStore> villageHeartRef,
      VillageStorageRequest request) {
    if (villageHeartRef == null || !villageHeartRef.isValid() || request == null ||
        !VillageStorageRequestType.DELIVER_TO_WORKPLACE.equals(request.getType()) ||
        request.getRequesterWorkplaceId() == null) {
      return false;
    }

    Store<ChunkStore> heartStore = villageHeartRef.getStore();
    VillageHeartComponent villageHeart = heartStore.getComponent(
        villageHeartRef,
        VillageHeartComponent.getComponentType());
    if (villageHeart == null) {
      return false;
    }

    WorkplaceComponent requesterWorkplace = villageHeart.getWorkplaceFromId(request.getRequesterWorkplaceId());
    if (requesterWorkplace == null || requesterWorkplace.getWorkplaceType() == StructureType.TOWN_HALL) {
      return false;
    }

    VillageStorageRequest lastRequest = requesterWorkplace.getLastStorageRequest();
    return lastRequest != null &&
        lastRequest.isRegisteredWithStorage() &&
        VillageStorageRequestType.DELIVER_TO_WORKPLACE.equals(lastRequest.getType()) &&
        request.getRequestId().equals(lastRequest.getRequestId());
  }

  private void prepareRequesterRetryRequest(
      Ref<ChunkStore> villageHeartRef,
      VillageStorageRequest previousRequest,
      VillageStorageRequest retryRequest) {
    if (previousRequest == null || retryRequest == null) {
      return;
    }

    retryRequest.setRegisteredWithStorage(false);
    retryRequest.setStorageWorkplaceId(null);
    retryRequest.addTriedStorageId(previousRequest.getStorageWorkplaceId());
    syncRequesterStorageRequest(villageHeartRef, previousRequest, retryRequest);
  }

  private void syncRequesterStorageRequest(
      Ref<ChunkStore> villageHeartRef,
      VillageStorageRequest currentRequest,
      VillageStorageRequest requestToStore) {
    if (currentRequest == null) {
      return;
    }

    syncRequesterStorageRequest(villageHeartRef, currentRequest.getRequestId(),
        currentRequest.getRequesterWorkplaceId(),
        requestToStore);
  }

  private void syncRequesterStorageRequest(
      Ref<ChunkStore> villageHeartRef,
      UUID currentRequestId,
      UUID requesterWorkplaceId,
      VillageStorageRequest requestToStore) {
    if (villageHeartRef == null || !villageHeartRef.isValid() || requesterWorkplaceId == null) {
      return;
    }

    Store<ChunkStore> heartStore = villageHeartRef.getStore();
    VillageHeartComponent villageHeart = heartStore.getComponent(
        villageHeartRef,
        VillageHeartComponent.getComponentType());
    if (villageHeart == null) {
      return;
    }

    WorkplaceComponent workplace = villageHeart.getWorkplaceFromId(requesterWorkplaceId);
    if (workplace == null) {
      return;
    }

    applyLastStorageRequest(workplace, currentRequestId, requestToStore);

    Ref<ChunkStore> requesterRef = workplace.getWorkplaceRef() != null ? workplace.getWorkplaceRef().getRef() : null;
    if (requesterRef != null && requesterRef.isValid()) {
      WorkplaceComponent persistedWorkplace = requesterRef.getStore().getComponent(
          requesterRef,
          WorkplaceComponent.getComponentType());
      applyLastStorageRequest(persistedWorkplace, currentRequestId, requestToStore);
      persistWorkplaceComponent(requesterRef, persistedWorkplace);
    }

    for (UUID requesterWorkerId : workplace.getWorkers()) {
      VillagerComponent villager = villageHeart.getVillager(requesterWorkerId);
      if (villager == null || villager.getWorkerData() == null) {
        continue;
      }

      applyLastStorageRequest(villager.getWorkerData().getWorkplace(), currentRequestId, requestToStore);
    }

    heartStore.putComponent(
        villageHeartRef,
        VillageHeartComponent.getComponentType(),
        villageHeart);
  }

  private void applyLastStorageRequest(
      WorkplaceComponent workplace,
      UUID currentRequestId,
      VillageStorageRequest requestToStore) {
    if (workplace == null) {
      return;
    }

    VillageStorageRequest lastRequest = workplace.getLastStorageRequest();
    if (lastRequest != null && currentRequestId != null && !currentRequestId.equals(lastRequest.getRequestId())) {
      return;
    }

    workplace.setLastStorageRequest(requestToStore != null ? requestToStore.clone() : null);
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

  private void persistStorageRequestComponent(
      Store<ChunkStore> chunkStore,
      Ref<ChunkStore> workplaceRef,
      WorkplaceVillageStorageRequestComponent storage) {
    chunkStore.putComponent(workplaceRef, WorkplaceVillageStorageRequestComponent.getComponentType(), storage);

    BlockModule.BlockStateInfo blockStateInfo = chunkStore.getComponent(
        workplaceRef,
        BlockModule.BlockStateInfo.getComponentType());
    if (blockStateInfo != null) {
      blockStateInfo.markNeedsSaving();
    }
  }

}
