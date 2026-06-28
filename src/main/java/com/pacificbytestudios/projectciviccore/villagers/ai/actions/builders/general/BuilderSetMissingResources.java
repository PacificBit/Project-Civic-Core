package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general;

import java.util.List;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderObjectListHelper;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general.SetMissingResources;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryItemCriteria;

import lombok.AccessLevel;
import lombok.Setter;

public class BuilderSetMissingResources extends BuilderActionBase {
  private BuilderObjectListHelper<InventoryItemCriteria> itemCriteria;
  @Setter(AccessLevel.PRIVATE)
  private Boolean resetFlag;

  @Override
  public Action build(BuilderSupport support) {
    return new SetMissingResources(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Sets or resets the missing resources in the workplace component";
  }

  @Override
  public String getShortDescription() {
    return "Sets or resets missing resources";
  }

  @Override
  public Builder<Action> readConfig(JsonElement data) {
    this.getBoolean(data, "ResetMissingResources", this::setResetFlag, false,
        BuilderDescriptorState.Experimental, "Resets the resources", null);

    this.getArray(data, "ItemCriteria", this.getSafeItemCriteria(), null,
        BuilderDescriptorState.Experimental, "List of item criteria used to match inventory items", null,
        this.validationHelper);

    return this;
  }

  public List<InventoryItemCriteria> getItemCriteria(BuilderSupport support) {
    return this.getSafeItemCriteria().build(support);
  }

  public boolean getResetMissingResourcesFlag() {
    return this.resetFlag;
  }

  private BuilderObjectListHelper<InventoryItemCriteria> getSafeItemCriteria() {
    if (this.itemCriteria == null) {
      this.itemCriteria = new BuilderObjectListHelper<>(InventoryItemCriteria.class, this);
    }
    return this.itemCriteria;
  }
}
