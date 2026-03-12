package com.pacificbytestudios.projectciviccore.villagers.core.inventory;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderBase;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.BooleanHolder;
import com.hypixel.hytale.server.npc.asset.builder.holder.IntHolder;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.IntRangeValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringNotEmptyValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringValidator;
import com.hypixel.hytale.server.npc.util.expression.ExecutionContext;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general.InteractWithExternalInventory.ItemCriteria;

public class BuilderItemCriteria extends BuilderBase<ItemCriteria> {

  private final StringHolder itemId = new StringHolder();
  private final BooleanHolder filterIn = new BooleanHolder();
  private final IntHolder count = new IntHolder();

  @Override
  public ItemCriteria build(BuilderSupport support) {
    return new ItemCriteria(
        this.itemId.get(support.getExecutionContext()),
        this.filterIn.get(support.getExecutionContext()),
        this.count.get(support.getExecutionContext()));
  }

  @Override
  public Class<ItemCriteria> category() {
    return ItemCriteria.class;
  }

  @Override
  public Builder<ItemCriteria> readConfig(JsonElement data) {
    this.requireString(data, "ItemId", this.itemId, (StringValidator) StringNotEmptyValidator.get(),
        BuilderDescriptorState.Experimental, "Item id to match", null);

    this.getBoolean(data, "FilterIn", this.filterIn, true, BuilderDescriptorState.Experimental,
        "Whether this criterion includes or excludes matching items", null);

    this.getInt(data, "Count", this.count, -1, IntRangeValidator.between(-1, Integer.MAX_VALUE),
        BuilderDescriptorState.Experimental, "Amount to withdraw/deposit. -1 means all matching items", null);

    return this;
  }

  @Override
  public boolean isEnabled(ExecutionContext context) {
    return true;
  }

  @Override
  public String getShortDescription() {
    return "External inventory item criterion";
  }

  @Override
  public String getLongDescription() {
    return this.getShortDescription();
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }
}
