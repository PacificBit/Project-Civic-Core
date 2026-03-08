package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general.ChangeIntoWorkRoleAction;

public class BuilderChangeIntoWorkRoleAction extends BuilderActionBase {

  @Override
  public Action build(BuilderSupport support) {
    return new ChangeIntoWorkRoleAction(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Change the Role of the villager to the correct one";
  }

  @Override
  public String getShortDescription() {
    return "Change the Role of the villager to the correct one";
  }

  public Builder<Action> readConfig(JsonElement data) {
    return this;
  }
}
