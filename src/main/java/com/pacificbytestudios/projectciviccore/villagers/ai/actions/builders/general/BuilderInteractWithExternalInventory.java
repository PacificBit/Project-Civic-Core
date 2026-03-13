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
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryItemCriteria;

public class BuilderInteractWithExternalInventory extends BuilderActionBase {
  private final BuilderObjectListHelper<InventoryItemCriteria> itemCriteria = new BuilderObjectListHelper<>(
      InventoryItemCriteria.class, this);

  private EnumHolder<ActionType> actionType = new EnumHolder<>();
  private Boolean consumeSnapshotTargetOnSuccess;
  private Boolean consumeSnapshotTargetOnFail;
  private Boolean useSnapshotItemCriteria;
  private String snapshotKey;

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

  @Override
  public Builder<Action> readConfig(JsonElement data) {
    this.requireEnum(data, "ActionType", this.actionType, InteractWithExternalInventory.ActionType.class,
        BuilderDescriptorState.Experimental, "Type of action to perform to the external inventory", null);

    this.requireString(data, "SnapshotKey", this::setSnapshotKey, (StringValidator) StringNotEmptyValidator.get(),
        BuilderDescriptorState.Experimental, "Snapshot of the current work (used to get the storage coordinate)", null);

    this.getBoolean(data, "ConsumeTargetOnSuccess", this::setConsumeSnapshotTargetOnSuccess, true,
        BuilderDescriptorState.Experimental, "Whether or not to remove a target from the snapshot after success", null);

    this.getBoolean(data, "ConsumeTargetOnFail", this::setConsumeSnapshotTargetOnFail, true,
        BuilderDescriptorState.Experimental, "Whether or not to remove a target from the snapshot after fail", null);

    this.getBoolean(data, "UseSnapshotItemCriteria", this::setUseSnapshotItemCriteria, false,
        BuilderDescriptorState.Experimental, "Whether or not to use the snapshot item filter criteria", null);

    this.getArray(data, "ItemCriteria", this.itemCriteria, ArrayNotEmptyValidator.get(),
        BuilderDescriptorState.Experimental, "List of item criteria used to match inventory items", null,
        this.validationHelper);

    return this;
  }

  private void setSnapshotKey(String key) {
    this.snapshotKey = key;
  }

  private void setConsumeSnapshotTargetOnSuccess(boolean flag) {
    this.consumeSnapshotTargetOnSuccess = flag;
  }

  private void setConsumeSnapshotTargetOnFail(boolean flag) {
    this.consumeSnapshotTargetOnFail = flag;
  }

  private void setUseSnapshotItemCriteria(boolean flag) {
    this.useSnapshotItemCriteria = flag;
  }

  public InteractWithExternalInventory.ActionType getActionType(BuilderSupport support) {
    return this.actionType.get(support.getExecutionContext());
  }

  public String getSnapshotKey() {
    return this.snapshotKey;
  }

  public List<InventoryItemCriteria> getItemCriteria(BuilderSupport support) {
    return this.itemCriteria.build(support);
  }

  public boolean getConsumeSnapshotTargetOnSuccessFlag() {
    return Boolean.TRUE.equals(this.consumeSnapshotTargetOnSuccess);
  }

  public boolean getConsumeSnapshotTargetOnFailFlag() {
    return Boolean.TRUE.equals(this.consumeSnapshotTargetOnFail);
  }

  public boolean getUseSnapshotItemCriteria() {
    return Boolean.TRUE.equals(this.useSnapshotItemCriteria);
  }
}
