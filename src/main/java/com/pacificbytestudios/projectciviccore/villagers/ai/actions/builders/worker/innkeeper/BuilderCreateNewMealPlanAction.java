
package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.innkeeper;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.innkeeper.CreateNewMealPlanAction;

public class BuilderCreateNewMealPlanAction extends BuilderActionBase {
  private String successFlag;
  private String failureFlag;

  @Override
  public Action build(BuilderSupport support) {
    return new CreateNewMealPlanAction(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Create new meal plan";
  }

  @Override
  public String getShortDescription() {
    return "Create new meal plan";
  }

  @Override
  public Builder<Action> readConfig(JsonElement data) {
    this.getString(data, "SuccessFlag", this::setSuccessFlag, "", null,
        BuilderDescriptorState.Experimental,
        "Optional flag set to true when the meal plan is available", null);

    this.getString(data, "FailureFlag", this::setFailureFlag, "", null,
        BuilderDescriptorState.Experimental,
        "Optional flag set to true when the meal plan could not be created", null);

    return this;
  }

  private void setSuccessFlag(String flag) {
    this.successFlag = flag;
  }

  private void setFailureFlag(String flag) {
    this.failureFlag = flag;
  }

  public int getSuccessFlagSlot(BuilderSupport support) {
    if (this.successFlag == null || this.successFlag.trim().isEmpty()) {
      return -1;
    }
    return support.getFlagSlot(this.successFlag);
  }

  public int getFailureFlagSlot(BuilderSupport support) {
    if (this.failureFlag == null || this.failureFlag.trim().isEmpty()) {
      return -1;
    }
    return support.getFlagSlot(this.failureFlag);
  }
}
