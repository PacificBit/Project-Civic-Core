package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class WatchForJobAssignment extends VillagerSensorBase {

  public WatchForJobAssignment(BuilderSensorBase builderSensorBase) {
    super(builderSensorBase);
  }

  @Override
  public InfoProvider getSensorInfo() {
    return null;
  }

  public boolean matches(
      Ref<EntityStore> ref,
      Role role,
      double deltaTime,
      Store<EntityStore> store) {
    if (!ref.isValid()) {
      return false;
    }
    return this.getWorkerDataComponent(ref, store) != null;
  }

}
