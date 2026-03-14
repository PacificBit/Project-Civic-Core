package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.innkeeper;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.innkeeper.GatherNextMealMaterialFromContainerAction;

public class BuilderGatherNextMealMaterialFromContainerAction extends BuilderActionBase {

  @Override
  public Action build(BuilderSupport support) {
    return new GatherNextMealMaterialFromContainerAction(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Cook next meal";
  }

  @Override
  public String getShortDescription() {
    return "Cook next meal";
  }

  public Builder<Action> readConfig(JsonElement data) {
    return this;
  }
}
