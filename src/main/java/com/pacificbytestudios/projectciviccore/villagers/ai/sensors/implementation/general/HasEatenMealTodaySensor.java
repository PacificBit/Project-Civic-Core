package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderHasEatenMealTodaySensor;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;
import com.pacificbytestudios.projectciviccore.villagers.enums.VillagerMealType;

public class HasEatenMealTodaySensor extends VillagerSensorBase {
  private final VillagerMealType mealType;

  public HasEatenMealTodaySensor(
      BuilderHasEatenMealTodaySensor builderSensorBase,
      BuilderSupport support) {
    super(builderSensorBase);
    this.mealType = builderSensorBase.getMealType(support);
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

    VillagerComponent villagerComponent = this.getVillagerComponent(ref, store);
    if (villagerComponent == null) {
      return false;
    }

    WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
    if (worldTimeResource == null) {
      return false;
    }

    long currentMealDay = VillagerComponent.buildMealDayKey(worldTimeResource.getGameDateTime());
    return villagerComponent.hasConsumedMealOnDay(this.mealType, currentMealDay);
  }
}
