package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderHasValidWorkAreaSnapshotSensor;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;

public class HasValidWorkAreaSnapshotSensor extends SensorBase {
  private final PositionProvider positionProvider = new PositionProvider();

  public HasValidWorkAreaSnapshotSensor(BuilderHasValidWorkAreaSnapshotSensor builder) {
    super(builder);
  }

  @Override
  public InfoProvider getSensorInfo() {
    return this.positionProvider;
  }

  public boolean matches(
      Ref<EntityStore> ref,
      Role role,
      double deltaTime,
      Store<EntityStore> store) {

    VillagerComponent villager = store.getComponent(ref, VillagerComponent.getComponentType());
    assert villager != null : "Could not find the villager's component";

    VillagerWorkerComponent workerData = villager.getWorkerData();
    assert workerData != null : "Could not find the villager's worker data";

    return workerData.getWorkareaSnapshot() != null &&
        workerData.getWorkareaSnapshot().isCompleted();
  }

}
