package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.mayor;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.villagers.components.MayorComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class HasPlanToExecuteToday extends VillagerSensorBase {

  public HasPlanToExecuteToday(BuilderSensorBase builderSensorBase) {
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

    MayorComponent mayorComponent = this.getMayorComponent(store, ref);

    return mayorComponent.hasProject();
  }

}
