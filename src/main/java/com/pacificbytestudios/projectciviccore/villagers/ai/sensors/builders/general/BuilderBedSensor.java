package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general;

import javax.annotation.Nonnull;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.Feature;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general.BedSensor;

public class BuilderBedSensor extends BuilderSensorBase {

  @Override
  public Sensor build(BuilderSupport support) {
    return new BedSensor(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Sets the target position to the bed coords";
  }

  @Override
  public String getShortDescription() {
    return "Sets the target position to the bed coords";
  }

  @Nonnull
  public Builder<Sensor> readConfig(JsonElement data) {
    this.provideFeature(Feature.Position);
    return this;
  }

}
