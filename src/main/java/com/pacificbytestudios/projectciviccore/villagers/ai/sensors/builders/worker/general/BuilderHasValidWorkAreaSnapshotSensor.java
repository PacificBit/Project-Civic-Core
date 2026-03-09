package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general;

import javax.annotation.Nonnull;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general.HasValidWorkAreaSnapshotSensor;

public class BuilderHasValidWorkAreaSnapshotSensor extends BuilderSensorBase {

  @Override
  public Sensor build(BuilderSupport support) {
    return new HasValidWorkAreaSnapshotSensor(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Search for a specific block in the working area";
  }

  @Override
  public String getShortDescription() {
    return "Search block in work area";
  }

  @Nonnull
  public Builder<Sensor> readConfig(JsonElement data) {
    return this;
  }

}
