package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general;

import javax.annotation.Nonnull;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringNotEmptyValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.StringValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general.EquipItemIfPossible;

public class BuilderEquipItemIfPossible extends BuilderSensorBase {

  private final StringHolder itemToEquip = new StringHolder();

  @Override
  public Sensor build(BuilderSupport support) {
    return new EquipItemIfPossible(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Equip item if possible";
  }

  @Override
  public String getShortDescription() {
    return "Equip item if possible";
  }

  @Nonnull
  public Builder<Sensor> readConfig(JsonElement data) {
    this.getString(data, "ItemToEquip", this.itemToEquip, null,
        (StringValidator) StringNotEmptyValidator.get(),
        BuilderDescriptorState.Stable,
        "Tool / item to equip", null);

    return this;
  }

  public String getItemToEquip(BuilderSupport support) {
    return this.itemToEquip.get(support.getExecutionContext());
  }
}
