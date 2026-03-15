package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.storagekeeper;

import java.util.ArrayDeque;
import java.util.UUID;
import java.util.function.Supplier;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.WorkplaceVillageStorageRequestComponent;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.storagekeeper.BuilderUpdateRequestStatusAction;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
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
    VillageHeartComponent villageHeart = this.getVillageHeartComponent(ref, store);
    if (workplaceRef == null || villageHeartRef == null || villageHeart == null) {
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
      clearCompletedRequesterState(villageHeart, assignedRequests);
      persistStorageRequestComponent(chunkStore, workplaceRef, storage);
      persistVillageHeartComponent(villageHeartRef, villageHeart);
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

  private void clearCompletedRequesterState(
      VillageHeartComponent villageHeart,
      Iterable<VillageStorageRequest> assignedRequests) {
    for (VillageStorageRequest request : assignedRequests) {
      if (request == null) {
        continue;
      }

      clearWorkplaceRequest(villageHeart.getWorkplaceFromId(request.getRequesterWorkplaceId()), request);

      WorkplaceComponent workplace = villageHeart.getWorkplaceFromId(request.getRequesterWorkplaceId());
      if (workplace == null) {
        continue;
      }

      for (UUID requesterWorkerId : workplace.getWorkers()) {
        VillagerComponent villager = villageHeart.getVillager(requesterWorkerId);
        if (villager == null || villager.getWorkerData() == null) {
          continue;
        }

        clearWorkplaceRequest(villager.getWorkerData().getWorkplace(), request);
      }
    }
  }

  private void clearWorkplaceRequest(WorkplaceComponent workplace, VillageStorageRequest completedRequest) {
    if (workplace == null) {
      return;
    }

    VillageStorageRequest lastRequest = workplace.getLastStorageRequest();
    if (lastRequest == null || !completedRequest.getRequestId().equals(lastRequest.getRequestId())) {
      return;
    }

    workplace.setLastStorageRequest(null);
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

  private void persistVillageHeartComponent(
      Ref<ChunkStore> villageHeartRef,
      VillageHeartComponent villageHeart) {
    villageHeartRef.getStore().putComponent(
        villageHeartRef,
        VillageHeartComponent.getComponentType(),
        villageHeart);
  }
}
