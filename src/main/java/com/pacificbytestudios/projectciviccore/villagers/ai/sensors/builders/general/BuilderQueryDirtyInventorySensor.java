package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general;

import javax.annotation.Nonnull;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.DoubleHolder;
import com.hypixel.hytale.server.npc.asset.builder.holder.StringArrayHolder;
import com.hypixel.hytale.server.npc.asset.builder.validators.DoubleSingleValidator;
import com.hypixel.hytale.server.npc.asset.builder.validators.DoubleValidator;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general.QueryDirtyInventorySensor;

public class BuilderQueryDirtyInventorySensor extends BuilderSensorBase {

  private StringArrayHolder excludeItemTypes = new StringArrayHolder();
  private final DoubleHolder minimumFullnessPercent = new DoubleHolder();

  @Override
  public Sensor build(BuilderSupport support) {
    return new QueryDirtyInventorySensor(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Used to query inventory";
  }

  @Override
  public String getShortDescription() {
    return "Used to query inventory";
  }

  @Nonnull
  public Builder<Sensor> readConfig(JsonElement data) {
    this.getStringArray(data, "ExcludeItemTypes", this.excludeItemTypes, null, 0, Integer.MAX_VALUE,
        null, BuilderDescriptorState.Stable, "Item types that do not mark inventory as dirty", null);
    this.getDouble(data, "MinimumFullnessPercent", this.minimumFullnessPercent, -1.0,
        (DoubleValidator) DoubleSingleValidator.greater(-1.01),
        BuilderDescriptorState.Stable,
        "Optional minimum slot fullness percent required before the sensor matches. Negative disables the threshold.",
        null);
    return this;
  }

  public String[] getExcludeItemTypes(BuilderSupport support) {
    return this.excludeItemTypes.get(support.getExecutionContext());
  }

  public double getMinimumFullnessPercent(BuilderSupport support) {
    return this.minimumFullnessPercent.get(support.getExecutionContext());
  }
}
