package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderHasValidWorkAreaSnapshotSensor;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

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
      Role role,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.matches(ref, role, deltaTime, store)) {
      return false;
    }

    WorkplaceAreaSnapshot workareaSnapshot = this.getAreaSnapshot(ref, store);
    if (workareaSnapshot == null) {
      return false;
    }

    WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
    if (worldTimeResource == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[HasValidWorkAreaSnapshotSensor] Could not fetch worldTimeResource");
      return false;
    }

    return workareaSnapshot.isValid(worldTimeResource);
  }

}
