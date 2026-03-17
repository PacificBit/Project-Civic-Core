package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.EnumHolder;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.CraftToolAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.CraftToolAction.ToolType;

public class BuilderCraftToolAction extends BuilderActionBase {
  private EnumHolder<ToolType> toolType = new EnumHolder<>();

  @Override
  public Action build(BuilderSupport support) {
    return new CraftToolAction(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Craft the selected tool";
  }

  @Override
  public String getShortDescription() {
    return "Craft selected tool";
  }

  @Override
  public Builder<Action> readConfig(JsonElement data) {
    this.requireEnum(data, "ToolType", this.toolType, ToolType.class,
        BuilderDescriptorState.Experimental,
        "Tool type to craft", null);
    return this;
  }

  public ToolType getToolType(BuilderSupport support) {
    return this.toolType.get(support.getExecutionContext());
  }
}
