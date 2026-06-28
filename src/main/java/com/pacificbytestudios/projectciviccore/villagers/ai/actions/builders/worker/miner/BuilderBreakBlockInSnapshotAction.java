package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.miner;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.DoubleHolder;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.DoubleSingleValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.DoubleValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringNotEmptyValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.miner.BreakBlockInSnapshotAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.miner.BreakBlockInSnapshotAction.DropType;

import lombok.AccessLevel;
import lombok.Setter;

public class BuilderBreakBlockInSnapshotAction extends BuilderActionBase {
  private final StringHolder primarySnapshotGroup = new StringHolder();
  private final StringHolder secondarySnapshotGroup = new StringHolder();
  private final DoubleHolder requiredStoppingRange = new DoubleHolder();
  private final DoubleHolder dropGenerationChance = new DoubleHolder();
  @Setter(AccessLevel.PRIVATE)
  private DropType dropType = DropType.MINER;

  @Override
  public Action build(BuilderSupport support) {
    return new BreakBlockInSnapshotAction(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Break block action";
  }

  @Override
  public String getShortDescription() {
    return "Break block action";
  }

  @Override
  public Builder<Action> readConfig(JsonElement data) {
    this.requireString(data, "PrimarySnapshotGroup", this.primarySnapshotGroup,
        (StringValidator) StringNotEmptyValidator.get(),
        BuilderDescriptorState.Experimental, "Snapshot group", null);

    this.requireString(data, "SecondarySnapshotGroup", this.secondarySnapshotGroup,
        (StringValidator) StringNotEmptyValidator.get(),
        BuilderDescriptorState.Experimental, "Snapshot group", null);

    this.getDouble(data, "RequiredStoppingRange", this.requiredStoppingRange, -1.0,
        (DoubleValidator) DoubleSingleValidator.greater(-1.01),
        BuilderDescriptorState.Experimental,
        "Range required before attempting to break the current face. Negative disables the guard.",
        null);

    this.getEnum(data, "DropType", this::setDropType, DropType.class, DropType.MINER,
        BuilderDescriptorState.Experimental,
        "Registry family used when generating a drop after a successful break.",
        null);

    this.getDouble(data, "DropGenerationChance", this.dropGenerationChance, 25.0,
        (DoubleValidator) DoubleSingleValidator.greater(-0.01),
        BuilderDescriptorState.Experimental,
        "Percent chance to generate a drop after a successful break. Values are clamped between 0 and 100.",
        null);

    return this;
  }

  public String getPrimarySnapshotGroup(BuilderSupport support) {
    return this.primarySnapshotGroup.get(support.getExecutionContext());
  }

  public String getSecondarySnapshotGroup(BuilderSupport support) {
    return this.secondarySnapshotGroup.get(support.getExecutionContext());
  }

  public double getRequiredStoppingRange(BuilderSupport support) {
    return this.requiredStoppingRange.get(support.getExecutionContext());
  }

  public DropType getDropType(BuilderSupport support) {
    return this.dropType;
  }

  public double getDropGenerationChance(BuilderSupport support) {
    return this.dropGenerationChance.get(support.getExecutionContext());
  }

}
