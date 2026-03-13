package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.storagekeeper;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.EnumHolder;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.storagekeeper.NotifyWorkplaceRequestStatusAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.storagekeeper.NotifyWorkplaceRequestStatusAction.StatusType;

public class BuilderNotifyWorkplaceRequestStatusAction extends BuilderActionBase {

  private EnumHolder<StatusType> requestStatus = new EnumHolder<>();

  @Override
  public Action build(BuilderSupport support) {
    return new NotifyWorkplaceRequestStatusAction(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Notify workplace of request success/fail";
  }

  @Override
  public String getShortDescription() {
    return "Notify workplace of request success/fail";
  }

  public Builder<Action> readConfig(JsonElement data) {
    this.requireEnum(data, "Status", this.requestStatus, StatusType.class,
        BuilderDescriptorState.Experimental,
        "Set the assign request to selected status", null);
    return this;
  }

  public StatusType getStatusType(BuilderSupport support) {
    return this.requestStatus.get(support.getExecutionContext());
  }
}
