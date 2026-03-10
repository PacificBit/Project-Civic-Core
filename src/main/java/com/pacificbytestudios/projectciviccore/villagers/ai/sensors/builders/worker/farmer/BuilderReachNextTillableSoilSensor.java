package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.farmer;

import javax.annotation.Nonnull;
import com.google.gson.JsonElement;

import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.Feature;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.farmer.ReachNextTillableSoilSensor;

@Deprecated(forRemoval = true)
public class BuilderReachNextTillableSoilSensor extends BuilderSensorBase {

  @Override
  public Sensor build(BuilderSupport support) {
    return new ReachNextTillableSoilSensor(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Sets the target position to the next tillable coords";
  }

  @Override
  public String getShortDescription() {
    return "Sets the target position to the next tillable coords";
  }

  @Nonnull
  public Builder<Sensor> readConfig(JsonElement data) {
    this.provideFeature(Feature.Position);
    return this;
  }
}
