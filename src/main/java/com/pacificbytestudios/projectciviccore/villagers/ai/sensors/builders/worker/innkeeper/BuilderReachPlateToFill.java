package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.innkeeper;

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
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.innkeeper.ReachPlateToFill;

public class BuilderReachPlateToFill extends BuilderSensorBase {
  private DoubleHolder stoppingRange = new DoubleHolder();

  @Override
  public Sensor build(BuilderSupport support) {
    return new ReachPlateToFill(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Sets the target to the plate that needs filling";
  }

  @Override
  public String getShortDescription() {
    return "Sets the target to the plate that needs filling";
  }

  @Override
  public Builder<Sensor> readConfig(JsonElement data) {
    this.provideFeature(Feature.Position);

    this.requireDouble(data, "StoppingRange", this.stoppingRange,
        (DoubleValidator) DoubleSingleValidator.greater(-0.01),
        BuilderDescriptorState.Stable, "Stopping range from stored position", null);

    return this;
  }

  public double getStoppingRange(BuilderSupport support) {
    return this.stoppingRange.get(support.getExecutionContext());
  }

}
