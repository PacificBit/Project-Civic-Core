package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringArrayHolder;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.RepairTerrainAction;

public class BuilderRepairTerrainAction extends BuilderActionBase {

  private final StringArrayHolder terrainRepairBlockIds = new StringArrayHolder();

  @Override
  public Action build(BuilderSupport support) {
    return new RepairTerrainAction(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Fix terrain with the given block type or deduct";
  }

  @Override
  public String getShortDescription() {
    return "Fix terrain with the given block type or deduct";
  }

  @Override
  public Builder<Action> readConfig(JsonElement data) {
    this.getStringArray(data, "TerrainRepairBlockIds", this.terrainRepairBlockIds, null, 0, Integer.MAX_VALUE,
        null, BuilderDescriptorState.Experimental,
        "Optional block ids used to refill missing terrain under and around the sapling before planting", null);
    return this;
  }

  public String[] getTerrainRepairBlockSet(BuilderSupport support) {
    return this.terrainRepairBlockIds.get(support.getExecutionContext());
  }
}
