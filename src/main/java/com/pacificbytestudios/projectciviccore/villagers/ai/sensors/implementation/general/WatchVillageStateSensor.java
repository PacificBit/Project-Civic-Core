package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.enums.VillageHeartState;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderWatchVillageStateSensor;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class WatchVillageStateSensor extends VillagerSensorBase {
  private final PositionProvider positionProvider = new PositionProvider();
  private VillageHeartState stateToListen;

  public WatchVillageStateSensor(BuilderWatchVillageStateSensor builderSensorBase) {
    super(builderSensorBase);
    this.stateToListen = builderSensorBase.getState();
  }

  public boolean matches(
      Ref<EntityStore> ref,
      Role role,
      double deltaTime,
      Store<EntityStore> store) {
    if (!ref.isValid()) {
      return false;
    }
    this.suppressWarnings = true;
    VillageHeartComponent villageHeart = this.getVillageHeartComponent(ref, store);
    if (villageHeart == null) {
      return false;
    }

    return this.stateToListen.equals(villageHeart.getState());
  }

  @Override
  public InfoProvider getSensorInfo() {
    return this.positionProvider;
  }

}
