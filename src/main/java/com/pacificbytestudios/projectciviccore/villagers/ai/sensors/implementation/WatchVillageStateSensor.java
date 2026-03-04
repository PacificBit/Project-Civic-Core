package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.state.VillageHeartState;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.BuilderWatchVillageStateSensor;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;

public class WatchVillageStateSensor extends SensorBase {
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
    VillagerComponent villagerComponent = store.getComponent(ref, VillagerComponent.getComponentType());
    Ref<ChunkStore> villageHearth = villagerComponent.getVillageHeartRef();

    if (villageHearth == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[WatchVillageStateSensor] Village heart ref is null");
      return false;
    }

    assert villageHearth.isValid();

    VillageHeartComponent component = villageHearth.getStore()
        .getComponent(villageHearth,
            VillageHeartComponent.getComponentType());

    assert component != null;

    return this.stateToListen.equals(component.getState());
  }

  @Override
  public InfoProvider getSensorInfo() {
    return this.positionProvider;
  }

}
