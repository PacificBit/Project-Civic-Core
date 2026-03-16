package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general.UnmountMountableAction;

public class BuilderUnmountMountableAction extends BuilderActionBase {

  @Override
  public Action build(BuilderSupport support) {
    return new UnmountMountableAction(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Unmount";
  }

  @Override
  public String getShortDescription() {
    return "Unmount";
  }

  @Override
  public Builder<Action> readConfig(JsonElement data) {
    return this;
  }
}
