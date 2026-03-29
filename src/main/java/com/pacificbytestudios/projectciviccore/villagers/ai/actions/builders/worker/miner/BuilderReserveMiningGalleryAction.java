package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.miner;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.miner.ReserveMiningGalleryAction;

public class BuilderReserveMiningGalleryAction extends BuilderActionBase {

  @Override
  public Action build(BuilderSupport support) {
    return new ReserveMiningGalleryAction(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Reserve mining gallery";
  }

  @Override
  public String getShortDescription() {
    return "Reserve mining gallery";
  }

  @Override
  public Builder<Action> readConfig(JsonElement data) {
    return this;
  }
}
