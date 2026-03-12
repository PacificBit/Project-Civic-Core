package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general;

import javax.annotation.Nonnull;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringArrayHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringArrayNotEmptyValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general.HasTargetBlockInSnapshotSensor;

public class BuilderHasTargetBlockInSnapshotSensor extends BuilderSensorBase {

  private StringArrayHolder targetBlockIds = new StringArrayHolder();

  @Override
  public Sensor build(BuilderSupport support) {
    return new HasTargetBlockInSnapshotSensor(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Search for a specific groups of block in the working area";
  }

  @Override
  public String getShortDescription() {
    return "Search block in work area";
  }

  @Nonnull
  public Builder<Sensor> readConfig(JsonElement data) {
    this.requireStringArray(data, "TargetBlockIds", this.targetBlockIds, 0, Integer.MAX_VALUE,
        StringArrayNotEmptyValidator.get(),
        BuilderDescriptorState.Experimental, "Reach blocks of a specific type if present in snapshot", null);
    return this;
  }

  public String[] getTargetBlockIds(BuilderSupport support) {
    return this.targetBlockIds.get(support.getExecutionContext());
  }

}
