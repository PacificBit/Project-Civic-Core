package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderTimeRangeSensor;

public class TimeRangeSensor extends SensorBase {

  private final int minMinutesOfDay;
  private final int maxMinutesOfDay;
  private final boolean wraps;

  public TimeRangeSensor(BuilderTimeRangeSensor builderSensorBase, BuilderSupport support) {
    super(builderSensorBase);

    double[] period = builderSensorBase.getPeriod(support);

    int minRaw = (int) Math.round(period[0] * 100.0);
    int maxRaw = (int) Math.round(period[1] * 100.0);

    this.minMinutesOfDay = (minRaw / 100) * 60 + (minRaw % 100);
    this.maxMinutesOfDay = (maxRaw / 100) * 60 + (maxRaw % 100);
    this.wraps = this.minMinutesOfDay > this.maxMinutesOfDay;
  }

  @Override
  public InfoProvider getSensorInfo() {
    return null;
  }

  @Override
  public boolean matches(
      Ref<EntityStore> ref,
      Role role,
      double dt,
      Store<EntityStore> store) {
    if (!super.matches(ref, role, dt, store)) {
      return false;
    }

    WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
    int currentMinutesOfDay = worldTimeResource.getGameDateTime().getHour() * 60
        + worldTimeResource.getGameDateTime().getMinute();

    return wraps
        ? currentMinutesOfDay >= minMinutesOfDay || currentMinutesOfDay <= maxMinutesOfDay
        : currentMinutesOfDay >= minMinutesOfDay && currentMinutesOfDay <= maxMinutesOfDay;
  }
}
