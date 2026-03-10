package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.Farmer;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.Farmer.WaterSoilAction;

public class BuilderWaterSoilAction extends BuilderActionBase {

  @Override
  public Action build(BuilderSupport support) {
    return new WaterSoilAction(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Water soil blocks present in the workarea snapshot";
  }

  @Override
  public String getShortDescription() {
    return "Water soil";
  }

  public Builder<Action> readConfig(JsonElement data) {
    return this;
  }
}
