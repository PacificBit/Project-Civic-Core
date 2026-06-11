package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderHasValidWorkAreaSnapshotSensor;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class HasValidWorkAreaSnapshotSensor extends VillagerSensorBase {
  private final PositionProvider positionProvider = new PositionProvider();

  public HasValidWorkAreaSnapshotSensor(BuilderHasValidWorkAreaSnapshotSensor builder) {
    super(builder);
  }

  @Override
  public InfoProvider getSensorInfo() {
    return this.positionProvider;
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

    WorkplaceAreaSnapshot workareaSnapshot = this.getAreaSnapshot(ref, store);
    if (workareaSnapshot == null) {
      return false;
    }

    WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
    if (worldTimeResource == null) {
      Logger.logWarning("Could not fetch worldTimeResource");
      return false;
    }

    return workareaSnapshot.isValid(worldTimeResource);
  }

}
