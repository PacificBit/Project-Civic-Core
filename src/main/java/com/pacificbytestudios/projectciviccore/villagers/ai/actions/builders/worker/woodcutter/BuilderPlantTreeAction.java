package com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.woodcutter;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringArrayHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringArrayNoEmptyStringsValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.woodcutter.PlantTreeAction;

public class BuilderPlantTreeAction extends BuilderActionBase {
  private final StringArrayHolder terrainRepairBlockIds = new StringArrayHolder();

  @Override
  public Action build(BuilderSupport support) {
    return new PlantTreeAction(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Plant sapling in hand";
  }

  @Override
  public String getShortDescription() {
    return "Plant sapling in hand";
  }

  @Override
  public Builder<Action> readConfig(JsonElement data) {
    this.getStringArray(data, "TerrainRepairBlockIds", this.terrainRepairBlockIds, null, 0, Integer.MAX_VALUE,
        StringArrayNoEmptyStringsValidator.get(), BuilderDescriptorState.Experimental,
        "Optional block ids used to refill missing terrain under and around the sapling before planting", null);
    return this;
  }

  public String[] getTerrainRepairBlockIds(BuilderSupport support) {
    return this.terrainRepairBlockIds.get(support.getExecutionContext());
  }
}
