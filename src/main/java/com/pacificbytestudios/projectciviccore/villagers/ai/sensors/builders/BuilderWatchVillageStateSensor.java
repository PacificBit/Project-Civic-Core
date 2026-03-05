package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders;

import javax.annotation.Nonnull;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringNotEmptyValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.village.enums.VillageHeartState;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.WatchVillageStateSensor;

public class BuilderWatchVillageStateSensor extends BuilderSensorBase {
  private VillageHeartState state;

  @Override
  public Sensor build(BuilderSupport support) {
    return new WatchVillageStateSensor(this);
  }

  public Builder<Sensor> readConfig(@Nonnull JsonElement data) {
    this.requireString(data, "State", this::setState, (StringValidator) StringNotEmptyValidator.get(),
        BuilderDescriptorState.Experimental,
        "State to listen, it should be one of: [" + VillageHeartState.getAvailableStates() + "]", null);
    return this;
  }

  private void setState(String state) {
    this.state = VillageHeartState.valueOf(state.toUpperCase());
  }

  public VillageHeartState getState() {
    return this.state;
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Used to monitor the village state which can be one of the following states: ["
        + VillageHeartState.getAvailableStates() + "]";
  }

  @Override
  public String getShortDescription() {
    return "Used to monitor the village state";
  }

}
