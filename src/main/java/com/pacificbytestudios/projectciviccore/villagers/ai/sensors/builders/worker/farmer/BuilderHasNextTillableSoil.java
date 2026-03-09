package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.farmer;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.farmer.HasTillableSoil;

public class BuilderHasNextTillableSoil extends BuilderSensorBase {

  @Override
  public Sensor build(BuilderSupport support) {
    return new HasTillableSoil(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Search in the work area snapshot the next tillable soil";
  }

  @Override
  public String getShortDescription() {
    return "Search tillable soil";
  }

  public Builder<Sensor> readConfig(JsonElement data) {
    return this;
  }

}
