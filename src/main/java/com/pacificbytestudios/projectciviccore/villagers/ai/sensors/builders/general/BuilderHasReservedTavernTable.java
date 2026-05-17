package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general.HasReservedTavernTable;

public class BuilderHasReservedTavernTable extends BuilderSensorBase {

  @Override
  public Sensor build(BuilderSupport support) {
    return new HasReservedTavernTable(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Has reserved table (it reserves one if possible)";
  }

  @Override
  public String getShortDescription() {
    return "Has reserved table (it reserves one if possible)";
  }

  @Override
  public Builder<Sensor> readConfig(JsonElement data) {
    return this;
  }

}
