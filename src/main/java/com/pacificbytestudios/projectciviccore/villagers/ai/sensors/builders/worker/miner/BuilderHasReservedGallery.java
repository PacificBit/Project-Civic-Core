package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.miner;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.miner.HasReservedGallery;

public class BuilderHasReservedGallery extends BuilderSensorBase {

  @Override
  public Sensor build(BuilderSupport support) {
    return new HasReservedGallery(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Check whether the miner has a gallery reserved to him";
  }

  @Override
  public String getShortDescription() {
    return "Check whether the miner has a gallery reserved to him";
  }

  @Override
  public Builder<Sensor> readConfig(JsonElement data) {
    return this;
  }

}
