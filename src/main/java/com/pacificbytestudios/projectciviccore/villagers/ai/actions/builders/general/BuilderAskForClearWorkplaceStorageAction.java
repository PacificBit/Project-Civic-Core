package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringArrayHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringArrayNotEmptyValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general.AskForClearWorkplaceStorageAction;

public class BuilderAskForClearWorkplaceStorageAction extends BuilderActionBase {

  private StringArrayHolder excludedItemTypes = new StringArrayHolder();

  @Override
  public Action build(BuilderSupport support) {
    return new AskForClearWorkplaceStorageAction(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Ask the village workplace to clear villager's workplace storage";
  }

  @Override
  public String getShortDescription() {
    return "Calls the storage keeper to clear workplace storage";
  }

  public Builder<Action> readConfig(JsonElement data) {
    this.requireStringArray(data, "ExcludedItemTypes", this.excludedItemTypes, 0, Integer.MAX_VALUE,
        StringArrayNotEmptyValidator.get(),
        BuilderDescriptorState.Experimental, "Item types that the storage keeper should not move", null);
    return this;
  }

  public String[] getExcludedItemTypes(BuilderSupport support) {
    return this.excludedItemTypes.get(support.getExecutionContext());
  }

}
