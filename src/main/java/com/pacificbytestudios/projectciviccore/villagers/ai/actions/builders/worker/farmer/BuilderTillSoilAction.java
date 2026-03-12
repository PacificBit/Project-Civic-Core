package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.farmer;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.farmer.TillSoilAction;

public class BuilderTillSoilAction extends BuilderActionBase {

  @Override
  public Action build(BuilderSupport support) {
    return new TillSoilAction(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Till next target soil";
  }

  @Override
  public String getShortDescription() {
    return "Till next target soil";
  }

  public Builder<Action> readConfig(JsonElement data) {
    return this;
  }
}
