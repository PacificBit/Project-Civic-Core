package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.mayor;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.mayor.ExecuteNextProjectStageAction;

public class BuilderExecuteNextProjectStageAction extends BuilderActionBase {

  @Override
  public Action build(BuilderSupport support) {
    return new ExecuteNextProjectStageAction(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Execute next project stage";
  }

  @Override
  public String getShortDescription() {
    return "Execute next project stage";
  }

  @Override
  public Builder<Action> readConfig(JsonElement data) {
    return this;
  }
}
