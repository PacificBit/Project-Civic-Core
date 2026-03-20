package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.mayor;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.mayor.HasPlanToExecuteToday;

public class BuilderHasPlanToExecuteToday extends BuilderSensorBase {

  @Override
  public Sensor build(BuilderSupport support) {
    return new HasPlanToExecuteToday(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Whether or not there are plans that needs to be executed";
  }

  @Override
  public String getShortDescription() {
    return "Whether or not there are plans that needs to be executed";
  }

  @Override
  public Builder<Sensor> readConfig(JsonElement data) {
    return this;
  }

}
