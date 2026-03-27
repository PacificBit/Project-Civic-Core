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
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general.ReachWalkableSpaceNearSnapshotTargetSensor;

public class BuilderReachWalkableSpaceNearSnapshotTargetSensor extends BuilderSensorBase {
  private final StringArrayHolder blockIds = new StringArrayHolder();
  private final DoubleHolder stoppingRange = new DoubleHolder();
  private Boolean useCenterOfBlock;

  @Override
  public Sensor build(BuilderSupport support) {
    return new ReachWalkableSpaceNearSnapshotTargetSensor(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Reach a walkable space near a block stored in the work area snapshot";
  }

  @Override
  public String getShortDescription() {
    return "Reach walkable space near snapshot block";
  }

  @Nonnull
  @Override
  public Builder<Sensor> readConfig(JsonElement data) {
    this.requireStringArray(
        data,
        "BlockIds",
        this.blockIds,
        0,
        Integer.MAX_VALUE,
        StringArrayNotEmptyValidator.get(),
        BuilderDescriptorState.Experimental,
        "Reach a walkable space near the first matching block in snapshot",
        null);

    this.requireDouble(
        data,
        "StoppingRange",
        this.stoppingRange,
        (DoubleValidator) DoubleSingleValidator.greater(-0.01),
        BuilderDescriptorState.Stable,
        "Stopping range from resolved walkable position",
        null);

    this.getBoolean(
        data,
        "UseCenterOfBlock",
        this::setUseCenterOfBlock,
        false,
        BuilderDescriptorState.Experimental,
        "Whether to seek the center of the resolved walkable block position",
        null);

    this.provideFeature(Feature.Position);
    return this;
  }

  public String[] getBlockIds(BuilderSupport support) {
    return this.blockIds.get(support.getExecutionContext());
  }

  public double getStoppingRange(BuilderSupport support) {
    return this.stoppingRange.get(support.getExecutionContext());
  }

  private void setUseCenterOfBlock(boolean useCenterOfBlock) {
    this.useCenterOfBlock = useCenterOfBlock;
  }

  public boolean getUseCenterOfBlock(BuilderSupport support) {
    return Boolean.TRUE.equals(this.useCenterOfBlock);
  }
}
