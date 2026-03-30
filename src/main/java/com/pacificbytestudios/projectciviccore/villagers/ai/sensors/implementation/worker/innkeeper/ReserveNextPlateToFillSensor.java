package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.innkeeper;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.Interactable;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceInteractibles;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class ReserveNextPlateToFillSensor extends VillagerSensorBase {

  public ReserveNextPlateToFillSensor(BuilderSensorBase builderSensorBase) {
    super(builderSensorBase);
  }

  @Override
  public InfoProvider getSensorInfo() {
    return null;
  }

  @Override
  public boolean matches(
      Ref<EntityStore> ref,
      Role role,
      double deltaTime,
      Store<EntityStore> store) {

    if (!super.matches(ref, role, deltaTime, store)) {
      return false;
    }

    WorkplaceInteractablesMapComponent interactables = this.getWorkplaceInteractibleComponent(ref, store);
    if (interactables == null) {
      return false;
    }

    UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
    if (uuidComponent == null) {
      ProjectCivicCore.LOGGER.atWarning().log(
          "[ReserveNextPlateToFillSensor] Missing UUIDComponent for entity");
      return false;
    }

    WorkplaceInteractibles group = WorkplaceInteractibles.EATING_SPOT;
    Interactable reservedInteractable = interactables.getInteractableOccupiedBy(group, uuidComponent.getUuid());
    if (reservedInteractable != null && !reservedInteractable.isReady()) {
      return true;
    }

    Interactable interactable = interactables.getFirstNotReadyInteractable(group, uuidComponent.getUuid());

    if (interactable == null) {
      ProjectCivicCore.LOGGER.atInfo().log(
          "[ReserveNextPlateToFillSensor] No more plates need filling");
      return false;
    }

    interactable.setOccupiedBy(uuidComponent.getUuid());
    return true;
  }
}
