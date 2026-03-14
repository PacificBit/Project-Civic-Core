package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker;

import java.util.List;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderObjectListHelper;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.EnumHolder;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.AskStorageForResourcesAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.AskStorageForResourcesAction.SourceType;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryItemCriteria;

public class BuilderAskStorageForResourcesAction extends BuilderActionBase {
  private EnumHolder<SourceType> sourceType = new EnumHolder<>();
  private final BuilderObjectListHelper<InventoryItemCriteria> itemCriteria = new BuilderObjectListHelper<>(
      InventoryItemCriteria.class, this);

  @Override
  public Action build(BuilderSupport support) {
    return new AskStorageForResourcesAction(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Ask storage for resources";
  }

  @Override
  public String getShortDescription() {
    return "Ask storage for resources";
  }

  public Builder<Action> readConfig(JsonElement data) {
    this.requireEnum(data, "SourceType", this.sourceType, SourceType.class,
        BuilderDescriptorState.Experimental,
        "What item criteria to use", null);

    this.getArray(data, "ItemCriteria", this.itemCriteria, null,
        BuilderDescriptorState.Experimental, "List of item criteria used to match inventory items", null,
        this.validationHelper);
    return this;
  }

  public SourceType getSourceType(BuilderSupport support) {
    return this.sourceType.get(support.getExecutionContext());
  }

  public List<InventoryItemCriteria> getItemCriteria(BuilderSupport support) {
    return this.itemCriteria.build(support);
  }

}
