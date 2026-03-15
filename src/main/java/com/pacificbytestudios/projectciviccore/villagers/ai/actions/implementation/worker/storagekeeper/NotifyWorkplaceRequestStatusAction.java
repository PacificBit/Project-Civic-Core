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
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.WorkplaceVillageStorageRequestComponent;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.storagekeeper.BuilderNotifyWorkplaceRequestStatusAction;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;

public class NotifyWorkplaceRequestStatusAction extends VillagerActionBase {
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

    Ref<ChunkStore> workplaceRef = this.getWorkplaceRef(ref, store);
    VillageHeartComponent villageHeart = this.getVillageHeartComponent(ref, store);
    if (workplaceRef == null || villageHeart == null) {
      return false;
    }

    UUID workerId = store.getComponent(ref, UUIDComponent.getComponentType()).getUuid();

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
