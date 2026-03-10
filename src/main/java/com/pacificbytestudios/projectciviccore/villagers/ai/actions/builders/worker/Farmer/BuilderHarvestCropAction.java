package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.Farmer;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.Farmer.HarvestCropAction;

public class BuilderHarvestCropAction extends BuilderActionBase {

  @Override
  public Action build(BuilderSupport support) {
    return new HarvestCropAction(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Harvest crops found in the internal snapshot";
  }

  @Override
  public String getShortDescription() {
    return "Harvest crops";
  }

  public Builder<Action> readConfig(JsonElement data) {
    return this;
  }

}
