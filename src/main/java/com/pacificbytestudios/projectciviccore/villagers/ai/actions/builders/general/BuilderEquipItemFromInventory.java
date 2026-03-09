package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringNotEmptyValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general.EquipItemFromInventory;

public class BuilderEquipItemFromInventory extends BuilderActionBase {

  private StringHolder itemIdToEquip = new StringHolder();

  @Override
  public Action build(BuilderSupport support) {
    return new EquipItemFromInventory(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Equip item from inventory";
  }

  @Override
  public String getShortDescription() {
    return "Equip item from inventory";
  }

  public Builder<Action> readConfig(JsonElement data) {
    this.getString(data, "ItemToEquip", this.itemIdToEquip, null,
        (StringValidator) StringNotEmptyValidator.get(),
        BuilderDescriptorState.Stable,
        "Item to equip", null);
    return this;
  }

  public String getItemIdToEquip(BuilderSupport support) {
    return this.itemIdToEquip.get(support.getExecutionContext());
  }

}
