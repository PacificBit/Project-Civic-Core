package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.innkeeper;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.innkeeper.ReserveNextPlateToFillSensor;

public class BuilderReserveNextPlateToFillSensor extends BuilderSensorBase {

  @Override
  public Sensor build(BuilderSupport support) {
    return new ReserveNextPlateToFillSensor(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Whether the tavern currently has a plate that needs to be filled with food.";
  }

  @Override
  public String getShortDescription() {
    return "Whether a plate needs food.";
  }

  @Override
  public Builder<Sensor> readConfig(JsonElement data) {
    return this;
  }

}
