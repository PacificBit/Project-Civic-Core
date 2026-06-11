package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.mayor;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.TownHallComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class HasNotDoneVillageAssessment extends VillagerSensorBase {

  public HasNotDoneVillageAssessment(BuilderSensorBase builderSensorBase) {
    super(builderSensorBase);
  }

  @Override
  public InfoProvider getSensorInfo() {
    return null;
  }

  @Override
  public boolean matches(
      Ref<EntityStore> ref,
      ExecutionSupport executionSupport,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.matches(ref, executionSupport, deltaTime, store)) {
      return false;
    }
    this.triggered = true;

    TownHallComponent townHall = this.getComponentFromWorkplace(store, ref, TownHallComponent.getComponentType());
    if (townHall == null) {
      return false;
    }

    WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
    int today = worldTimeResource.getGameDateTime().getDayOfYear();

    return townHall.getLastDayVillageAssessment() != today;
  }

}
