package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.EnumHolder;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general.HasEatenMealTodaySensor;
import com.pacificbytestudios.projectciviccore.villagers.enums.VillagerMealType;

public class BuilderHasEatenMealTodaySensor extends BuilderSensorBase {
  private final EnumHolder<VillagerMealType> mealType = new EnumHolder<>();

  @Override
  public Sensor build(BuilderSupport support) {
    return new HasEatenMealTodaySensor(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Returns true if the villager has already consumed the selected meal today";
  }

  @Override
  public String getShortDescription() {
    return "Checks whether the villager already ate the selected meal today";
  }

  @Override
  public Builder<Sensor> readConfig(JsonElement data) {
    this.requireEnum(data, "MealType", this.mealType, VillagerMealType.class,
        BuilderDescriptorState.Experimental, "Meal slot to evaluate", null);
    return this;
  }

  public VillagerMealType getMealType(BuilderSupport support) {
    return this.mealType.get(support.getExecutionContext());
  }
}
