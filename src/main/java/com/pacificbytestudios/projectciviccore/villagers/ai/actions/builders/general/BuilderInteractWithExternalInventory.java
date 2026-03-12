package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general;

import java.util.List;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderObjectListHelper;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.EnumHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.ArrayNotEmptyValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringNotEmptyValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general.InteractWithExternalInventory;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general.InteractWithExternalInventory.ActionType;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general.InteractWithExternalInventory.ItemCriteria;

public class BuilderInteractWithExternalInventory extends BuilderActionBase {
  private EnumHolder<ActionType> actionType = new EnumHolder<>();
  private String snapshotKey;
  private final BuilderObjectListHelper<ItemCriteria> itemCriteria = new BuilderObjectListHelper<>(
      ItemCriteria.class, this);

  @Override
  public Action build(BuilderSupport support) {
    return new InteractWithExternalInventory(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Fetch items from external inventory";
  }

  @Override
  public String getShortDescription() {
    return "Fetch items from external inventory";
  }

  public Builder<Action> readConfig(JsonElement data) {

    this.requireEnum(data, "ActionType", this.actionType, InteractWithExternalInventory.ActionType.class,
        BuilderDescriptorState.Experimental,
        "Type of actio to perform to the external inventory", null);

    this.requireString(data, "SnapshotKey", this::setSnapshotKey, (StringValidator) StringNotEmptyValidator.get(),
        BuilderDescriptorState.Experimental,
        "Snapshot of the current work (used to get the storage coordinate)", null);

    this.getArray(data, "ItemCriteria", this.itemCriteria, ArrayNotEmptyValidator.get(),
        BuilderDescriptorState.Experimental, "List of item criteria used to match inventory items",
        null, this.validationHelper);

    return this;
  }

  private void setSnapshotKey(String key) {
    this.snapshotKey = key;
  }

  public InteractWithExternalInventory.ActionType getActionType(BuilderSupport support) {
    return this.actionType.get(support.getExecutionContext());
  }

  public String getSnapshotKey() {
    return this.snapshotKey;
  }

  public List<ItemCriteria> getItemCriteria(BuilderSupport support) {
    return this.itemCriteria.build(support);
  }

}
