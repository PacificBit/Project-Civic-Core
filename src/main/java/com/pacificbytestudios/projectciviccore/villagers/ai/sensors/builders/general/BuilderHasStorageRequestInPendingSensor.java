package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general.HasStorageRequestInPendingSensor;

public class BuilderHasStorageRequestInPendingSensor extends BuilderSensorBase {

  @Override
  public Sensor build(BuilderSupport support) {
    return new HasStorageRequestInPendingSensor(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Checks wheter the workplace has a storage request in pending";
  }

  @Override
  public String getShortDescription() {
    return "Checks wheter the workplace has a storage request in pending";
  }

  public Builder<Sensor> readConfig(JsonElement data) {
    return this;
  }

}
