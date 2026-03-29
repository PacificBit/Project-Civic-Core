package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.miner;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.BooleanHolder;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.miner.UnregisterFromMineshaftGallery;

public class BuilderUnregisterFromMineshaftGallery extends BuilderActionBase {
  private final BooleanHolder markAsCompleted = new BooleanHolder();

  @Override
  public Action build(BuilderSupport support) {
    return new UnregisterFromMineshaftGallery(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Unregister worker from gallery";
  }

  @Override
  public String getShortDescription() {
    return "Unregister worker from gallery";
  }

  @Override
  public Builder<Action> readConfig(JsonElement data) {
    this.getBoolean(data, "MarkAsCompleted", this.markAsCompleted, false, BuilderDescriptorState.Experimental,
        "Mark gallery as completed", null);
    return this;
  }

  public Boolean getMarkAsCompleted(BuilderSupport support) {
    return this.markAsCompleted.get(support.getExecutionContext());
  }

}
