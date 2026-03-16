package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.EnumHolder;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.WorkplaceStorageTargetsSnapshotAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.WorkplaceStorageTargetsSnapshotAction.ActionType;

public class BuilderWorkplaceStorageTargetsSnapshotAction extends BuilderActionBase {
  private EnumHolder<ActionType> actionType = new EnumHolder<>();

  @Override
  public Action build(BuilderSupport support) {
    return new WorkplaceStorageTargetsSnapshotAction(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Adds or Remove from the available work area snapshot the workplace storage locations";
  }

  @Override
  public String getShortDescription() {
    return "Adds or Remove workplace storage in snapshot";
  }

  public Builder<Action> readConfig(JsonElement data) {
    this.requireEnum(data, "ActionType", this.actionType, ActionType.class,
        BuilderDescriptorState.Experimental,
        "Wheter to add or remove workplace storage from snapshot", null);

    return this;
  }

  public ActionType getActionType(BuilderSupport support) {
    return this.actionType.get(support.getExecutionContext());
  }

}
