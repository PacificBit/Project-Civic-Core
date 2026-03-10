package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringNotEmptyValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general.ClearSnapshotGroupPositions;

public class BuilderClearSnapshotGroupPositions extends BuilderActionBase {

  private StringHolder groupName = new StringHolder();

  @Override
  public Action build(BuilderSupport support) {
    return new ClearSnapshotGroupPositions(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Removes all coordinates stored under the specified group key from the workplace area snapshot.";
  }

  @Override
  public String getShortDescription() {
    return "Clear snapshot group positions";
  }

  public Builder<Action> readConfig(JsonElement data) {
    this.getString(data, "GroupName", this.groupName, null,
        (StringValidator) StringNotEmptyValidator.get(),
        BuilderDescriptorState.Stable,
        "Snapshot group key", null);
    return this;
  }

  public String getGroupName(BuilderSupport support) {
    return this.groupName.get(support.getExecutionContext());
  }

}
