package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;

public class UnmountMountableAction extends VillagerActionBase {

  public UnmountMountableAction(BuilderActionBase builderActionBase) {
    super(builderActionBase);
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

    System.out.println("Dismantlying");
    UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
    WorkplaceInteractablesMapComponent tavernInteractables = this.getVillageTavernInteractibleComponent(ref, store);
    if (uuidComponent != null && tavernInteractables != null) {
      tavernInteractables.releaseInteractablesOccupiedBy(uuidComponent.getUuid());
    }

    store.tryRemoveComponent(ref, MountedComponent.getComponentType());
    return true;
  }

}
