package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general;

import javax.annotation.Nonnull;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.NumberArrayHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.DoubleArrayValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.DoubleSequenceValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general.TimeRangeSensor;

public class BuilderTimeRangeSensor extends BuilderSensorBase {
  protected final NumberArrayHolder period = new NumberArrayHolder();

  public double[] getPeriod(BuilderSupport support) {
    return this.period.get(support.getExecutionContext());
  }

  @Override
  public Sensor build(BuilderSupport support) {
    return new TimeRangeSensor(this, support);
  }

  @Override
  public Builder<Sensor> readConfig(@Nonnull JsonElement data) {
    this.requireDoubleRange(data, "Period", this.period,
        (DoubleArrayValidator) DoubleSequenceValidator.between(0.0, 24.0), BuilderDescriptorState.Stable,
        "The time period to trigger within", null);
    return this;
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Returs true if current time is in range. Uses military format. A single hour goes from 0 to 0.59";
  }

  @Override
  public String getShortDescription() {
    return "Returns true if current time is in range.";
  }

}
