package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.RequestNewTaskComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.core.workplacetask.IWorkplaceTaskStrategy;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderRequestNewTaskAction;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;

public class RequestNewTaskAction extends ActionBase {

  public RequestNewTaskAction(BuilderRequestNewTaskAction builderActionBase) {
    super(builderActionBase);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {

    VillagerComponent villager = store.getComponent(ref, VillagerComponent.getComponentType());

    if (villager == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[RequestNewTaskAction] Could not fetch the villagerComponent");
      return false;
    }

    WorkplaceComponent workplace = villager.getWorkerData().getWorkplace();

    if (workplace == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[RequestNewTaskAction] Could not fetch the workplaceComponent");
      return false;
    }

    Ref<ChunkStore> workplaceRef = workplace.getWorkplaceRef().getRef();

    if (workplaceRef == null || !workplaceRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[RequestNewTaskAction] Could not fetch the workplace reference");
      return false;
    }

    UUIDComponent villagerUUID = store.getComponent(ref, UUIDComponent.getComponentType());

    if (villagerUUID == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[RequestNewTaskAction] Could not fetch the villager identifier");
      return false;
    }

    Store<ChunkStore> chunkStore = workplaceRef.getStore();
    RequestNewTaskComponent request = chunkStore.getComponent(
        workplaceRef,
        RequestNewTaskComponent.getComponentType());

    WorkplaceComponent workplaceComponent = chunkStore.getComponent(
        workplaceRef,
        WorkplaceComponent.getComponentType());

    IWorkplaceTaskStrategy strategy = Utils.getWorkplaceTaskStrategy(workplaceComponent.getWorkplaceType());
    //
    // UUID uuid = villagerUUID.getUuid();
    // if (request == null) {
    // request = new RequestNewTaskComponent(strategy);
    // request.addWorkerId(uuid);
    // chunkStore.putComponent(
    // workplaceRef,
    // RequestNewTaskComponent.getComponentType(),
    // request);
    // } else {
    // request.addWorkerId(uuid);
    // }

    return true;
  }

}
