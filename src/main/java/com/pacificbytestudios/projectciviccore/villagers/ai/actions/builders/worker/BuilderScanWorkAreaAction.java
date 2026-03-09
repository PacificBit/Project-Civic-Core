package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.ScanWorkAreaAction;

public class BuilderScanWorkAreaAction extends BuilderActionBase {

  @Override
  public Action build(BuilderSupport support) {
    return new ScanWorkAreaAction(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Scan gradually the whole assigned work area";
  }

  @Override
  public String getShortDescription() {
    return "Scan work area";
  }

  public Builder<Action> readConfig(JsonElement data) {
    return this;
  }

}
