package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general;

import javax.annotation.Nonnull;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general.RequestNewTaskAction;

public class BuilderRequestNewTaskAction extends BuilderActionBase {

  @Override
  public Action build(BuilderSupport support) {
    return new RequestNewTaskAction(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Request new action to the workplace";
  }

  @Override
  public String getShortDescription() {
    return "Request new action to the workplace";
  }

  public Builder<Action> readConfig(@Nonnull JsonElement data) {
    return this;
  }
}
