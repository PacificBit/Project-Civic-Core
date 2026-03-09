package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.systems.RoleChangeSystem;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.enums.WorkType;

public class ChangeIntoWorkRoleAction extends ActionBase {

  private static final String DEFAULT_STATE = "Working";
  private static final String DEFAULT_SUBSTATE = "Default";

  public ChangeIntoWorkRoleAction(BuilderActionBase builderActionBase) {
    super(builderActionBase);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    super.execute(ref, role, sensorInfo, deltaTime, store);

    VillagerWorkerComponent workerData = store.getComponent(ref,
        VillagerComponent.getComponentType())
        .getWorkerData();

    if (workerData == null) {
      return false;
    }

    WorkType workType = workerData.getTypeOfWorker();

    int roleIndex = NPCPlugin.get().getIndex(workType.getRoleId());

    RoleChangeSystem.requestRoleChange(ref, role, roleIndex, false, DEFAULT_STATE, DEFAULT_SUBSTATE, store);
    role.setReachedTerminalAction(true);

    return true;
  }

}
