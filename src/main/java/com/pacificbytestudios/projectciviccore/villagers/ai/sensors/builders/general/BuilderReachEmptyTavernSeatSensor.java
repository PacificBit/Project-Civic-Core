package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.Feature;
import com.hypixel.hytale.server.npc.asset.builder.holder.DoubleHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.DoubleSingleValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.DoubleValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general.ReachEmptyTavernSeatSensor;

public class BuilderReachEmptyTavernSeatSensor extends BuilderSensorBase {

  private DoubleHolder stoppingRange = new DoubleHolder();

  @Override
  public Sensor build(BuilderSupport support) {
    return new ReachEmptyTavernSeatSensor(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Reaches the next empty tavern seat";
  }

  @Override
  public String getShortDescription() {
    return "Reaches the next empty tavern seat";
  }

  @Override
  public Builder<Sensor> readConfig(JsonElement data) {
    this.provideFeature(Feature.Position);

    this.requireDouble(data, "StoppingRange", this.stoppingRange,
        (DoubleValidator) DoubleSingleValidator.greater(-0.01),
        BuilderDescriptorState.Stable, "Stopping range from stored position", null);

    return super.readConfig(data);
  }

  public double getStoppingRange(BuilderSupport support) {
    return this.stoppingRange.get(support.getExecutionContext());
  }
}
