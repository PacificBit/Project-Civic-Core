package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.woodcutter;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.DoubleHolder;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringArrayHolder;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.DoubleSingleValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.DoubleValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringArrayNoEmptyStringsValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringNotEmptyValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.woodcutter.BreakTreeBlockAction;

public class BuilderBreakTreeBlockAction extends BuilderActionBase {
  private final StringHolder snapshotGroup = new StringHolder();
  private final DoubleHolder requiredStoppingRange = new DoubleHolder();
  private final StringArrayHolder terrainRepairBlockIds = new StringArrayHolder();

  @Override
  public Action build(BuilderSupport support) {
    return new BreakTreeBlockAction(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Break block in snapshotGroup";
  }

  @Override
  public String getShortDescription() {
    return "Break block in snapshotGroup";
  }

  @Override
  public Builder<Action> readConfig(JsonElement data) {
    this.requireString(data, "SnapshotGroup", this.snapshotGroup, (StringValidator) StringNotEmptyValidator.get(),
        BuilderDescriptorState.Experimental, "Snapshot group", null);

    this.getDouble(data, "RequiredStoppingRange", this.requiredStoppingRange, -1.0,
        (DoubleValidator) DoubleSingleValidator.greater(-1.01),
        BuilderDescriptorState.Experimental,
        "Range required before attempting to break the target block. Negative disables the guard.",
        null);

    this.getStringArray(data, "TerrainRepairBlockIds", this.terrainRepairBlockIds, null, 0, Integer.MAX_VALUE,
        StringArrayNoEmptyStringsValidator.get(), BuilderDescriptorState.Experimental,
        "Optional block ids used to refill missing terrain under and around the sapling before planting", null);
    return this;
  }

  public String getSnapshotGroup(BuilderSupport support) {
    return this.snapshotGroup.get(support.getExecutionContext());
  }

  public double getRequiredStoppingRange(BuilderSupport support) {
    return this.requiredStoppingRange.get(support.getExecutionContext());
  }

  public String[] getTerrainRepairBlockIds(BuilderSupport support) {
    return this.terrainRepairBlockIds.get(support.getExecutionContext());
  }
}
