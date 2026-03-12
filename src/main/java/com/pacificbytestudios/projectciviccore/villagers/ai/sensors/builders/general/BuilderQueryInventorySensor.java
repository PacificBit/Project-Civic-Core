package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general;

import javax.annotation.Nonnull;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.Feature;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general.QueryInventorySensor;

public class BuilderQueryInventorySensor extends BuilderSensorBase {

  @Override
  public Sensor build(BuilderSupport support) {
    return new QueryInventorySensor(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Used to query inventory";
  }

  @Override
  public String getShortDescription() {
    return "Used to query inventory";
  }

  @Nonnull
  public Builder<Sensor> readConfig(JsonElement data) {
    this.provideFeature(Feature.Position);
    return this;
  }
}
