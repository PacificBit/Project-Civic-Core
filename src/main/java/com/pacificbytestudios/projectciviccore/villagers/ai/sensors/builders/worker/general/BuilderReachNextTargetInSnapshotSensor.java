package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general;

import javax.annotation.Nonnull;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.Feature;
import com.hypixel.hytale.server.npc.asset.builder.holder.DoubleHolder;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringArrayHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.DoubleSingleValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.DoubleValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringArrayNotEmptyValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general.ReachNextTargetInSnapshotSensor;

public class BuilderReachNextTargetInSnapshotSensor extends BuilderSensorBase {

  private StringArrayHolder blockIds = new StringArrayHolder();
  private DoubleHolder stoppingRange = new DoubleHolder();

  @Override
  public Sensor build(BuilderSupport support) {
    return new ReachNextTargetInSnapshotSensor(this, support);
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
    this.requireStringArray(data, "BlockIds", this.blockIds, 0, Integer.MAX_VALUE, StringArrayNotEmptyValidator.get(),
        BuilderDescriptorState.Experimental, "Reach blocks of a specific type if present in snapshot", null);

    this.requireDouble(data, "StoppingRange", this.stoppingRange,
        (DoubleValidator) DoubleSingleValidator.greater(-0.01),
        BuilderDescriptorState.Stable, "Stopping range from stored position", null);
    this.provideFeature(Feature.Position);
    return this;
  }

  public String[] getBlockIds(BuilderSupport support) {
    return this.blockIds.get(support.getExecutionContext());
  }

  public double getStoppingRange(BuilderSupport support) {
    return this.stoppingRange.get(support.getExecutionContext());
  }

}
