package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.storagekeeper;

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
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.WorkplaceVillageStorageRequestComponent;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.storagekeeper.BuilderUpdateRequestStatusAction;
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
    Ref<ChunkStore> workplaceRef = this.getWorkplaceRef(ref, store);
    if (workplaceRef == null) {
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

    if (this.status.equals(StatusType.COMPLETED)) {
      return storage.removeCompletedRequest(workerId);
    }

    return storage.unassignRequest(workerId);
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
}
