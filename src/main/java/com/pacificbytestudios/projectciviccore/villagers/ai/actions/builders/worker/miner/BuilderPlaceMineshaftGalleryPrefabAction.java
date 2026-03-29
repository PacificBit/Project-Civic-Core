package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.miner;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.EnumHolder;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringNotEmptyValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.miner.PlaceMineshaftGalleryPrefabAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.miner.PlaceMineshaftGalleryPrefabAction.PrefabType;

public class BuilderPlaceMineshaftGalleryPrefabAction extends BuilderActionBase {
  private final EnumHolder<PrefabType> prefabType = new EnumHolder<>();
  private final StringHolder snapshotAnchorGroup = new StringHolder();
  private final StringHolder commitLastDiggedFromSnapshotGroup = new StringHolder();

  @Override
  public Action build(BuilderSupport support) {
    return new PlaceMineshaftGalleryPrefabAction(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Place mineshaft gallery prefab";
  }

  @Override
  public String getShortDescription() {
    return "Place mineshaft gallery prefab";
  }

  @Override
  public Builder<Action> readConfig(JsonElement data) {
    this.requireEnum(data, "PrefabType", this.prefabType, PrefabType.class,
        BuilderDescriptorState.Experimental,
        "Whether to capture or place a gallery prefab section", null);

    this.getString(data, "SnapshotAnchorGroup", this.snapshotAnchorGroup, null,
        (StringValidator) StringNotEmptyValidator.get(),
        BuilderDescriptorState.Experimental,
        "Optional snapshot group containing a single prefab anchor position", null);

    this.getString(data, "CommitLastDiggedFromSnapshotGroup", this.commitLastDiggedFromSnapshotGroup, null,
        null, BuilderDescriptorState.Experimental,
        "Optional snapshot group containing the new gallery last-digged position to commit after placement", null);

    return this;
  }

  public PrefabType getPrefabType(BuilderSupport support) {
    return this.prefabType.get(support.getExecutionContext());
  }

  public String getSnapshotAnchorGroup(BuilderSupport support) {
    return this.snapshotAnchorGroup.get(support.getExecutionContext());
  }

  public String getCommitLastDiggedFromSnapshotGroup(BuilderSupport support) {
    return this.commitLastDiggedFromSnapshotGroup.get(support.getExecutionContext());
  }
}
