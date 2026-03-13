package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.storagekeeper;

import java.util.ArrayDeque;
import java.util.UUID;
import java.util.function.Supplier;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.WorkplaceVillageStorageRequestComponent;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.storagekeeper.BuilderNotifyWorkplaceRequestStatusAction;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;

public class NotifyWorkplaceRequestStatusAction extends ActionBase {
  // private StatusType status;

  public NotifyWorkplaceRequestStatusAction(
      BuilderNotifyWorkplaceRequestStatusAction builderActionBase,
      BuilderSupport support) {
    super(builderActionBase);

    // TODO: in the future use the status
    // this.status = builderActionBase.getStatusType(support);
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

    VillagerComponent villager = store.getComponent(ref, VillagerComponent.getComponentType());
    if (villager == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[NotifyWorkplaceRequestStatusAction] Could not fetch villager component");
      return false;
    }

    UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
    if (uuidComponent == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[NotifyWorkplaceRequestStatusAction] Could not fetch UUID component");
      return false;
    }

    UUID workerId = uuidComponent.getUuid();

    VillagerWorkerComponent workerData = villager.getWorkerData();
    if (workerData == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[NotifyWorkplaceRequestStatusAction] Could not fetch worker data for worker " + workerId);
      return false;
    }

    WorkplaceComponent workplace = workerData.getWorkplace();
    if (workplace == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[NotifyWorkplaceRequestStatusAction] Could not fetch workplace for worker " + workerId);
      return false;
    }

    Ref<ChunkStore> workplaceRef = workplace.getWorkplaceRef().getRef();
    if (workplaceRef == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[NotifyWorkplaceRequestStatusAction] Workplace ref is null for worker " + workerId);
      return false;
    }

    Store<ChunkStore> chunkStore = workplaceRef.getStore();
    WorkplaceVillageStorageRequestComponent storage = chunkStore.getComponent(
        workplaceRef,
        WorkplaceVillageStorageRequestComponent.getComponentType());

    if (storage == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log(
              "[NotifyWorkplaceRequestStatusAction] Could not fetch workplace storage request component for workplace ref "
                  + workplaceRef);
      return false;
    }

    Ref<ChunkStore> villageHeartRef = workplace.getVillageHeartRef();
    if (villageHeartRef == null || !villageHeartRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere()
          .log(
              "[NotifyWorkplaceRequestStatusAction] Could not fetch village heart");
      return false;
    }

    VillageHeartComponent villageHeart = chunkStore.getComponent(villageHeartRef,
        VillageHeartComponent.getComponentType());

    ArrayDeque<VillageStorageRequest> storageRequests = storage.getAssignedRequest(workerId);

    for (VillageStorageRequest request : storageRequests) {
      UUID requesterId = request.getRequesterWorkplaceId();
      WorkplaceComponent requesterWorkplace = villageHeart.getWorkplaceFromId(requesterId);
      // TODO: this should work be written to handle failed tasks.
      requesterWorkplace.setLastStorageRequest(null);
    }

    return true;
  }

  public enum StatusType implements Supplier<String> {
    COMPLETED("completed"),
    FAILED("failed");

    private final String type;

    StatusType(String type) {
      this.type = type;
    }

    @Override
    public String get() {
      return this.type;
    }
  }
}
