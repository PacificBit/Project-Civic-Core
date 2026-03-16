package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.innkeeper;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.innkeeper.PlaceFoodOnPlateAction;

public class BuilderPlaceFoodOnPlateAction extends BuilderActionBase {

  @Override
  public Action build(BuilderSupport support) {
    return new PlaceFoodOnPlateAction(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Place food on plate";
  }

  @Override
  public String getShortDescription() {
    return "Place food on plate";
  }

  @Override
  public Builder<Action> readConfig(JsonElement data) {
    return this;
  }

}
