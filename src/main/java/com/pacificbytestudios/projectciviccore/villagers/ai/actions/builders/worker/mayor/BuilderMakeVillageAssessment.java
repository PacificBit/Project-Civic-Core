package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.mayor;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.mayor.MakeVillageAssessment;

public class BuilderMakeVillageAssessment extends BuilderActionBase {

  @Override
  public Action build(BuilderSupport support) {
    return new MakeVillageAssessment(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Make new village status assessment (Used to deside what to build / upgrade / destroy today)";
  }

  @Override
  public String getShortDescription() {
    return "Make new village status assessment";
  }

  @Override
  public Builder<Action> readConfig(JsonElement data) {
    return this;
  }
}
