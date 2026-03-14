package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.innkeeper;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.BooleanHolder;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.innkeeper.HasNextMealInPlannedSensor;

public class BuilderHasNextMealInPlannedSensor extends BuilderSensorBase {

  private BooleanHolder assignMealToSelf = new BooleanHolder();

  @Override
  public Sensor build(BuilderSupport support) {
    return new HasNextMealInPlannedSensor(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Return wheter or not there is a new meal in the meal plan";
  }

  @Override
  public String getShortDescription() {
    return "Return wheter or not there is a new meal in the meal plan";
  }

  @Nonnull
  public Builder<Sensor> readConfig(JsonElement data) {
    this.getBoolean(data, "AssignMealToSelf", this.assignMealToSelf, false, BuilderDescriptorState.Experimental,
        "Whether to register the NPC as the next cook for the next available meal (if present)", null);
    return this;
  }

  public boolean getAssignMealToSelfFlag(BuilderSupport support) {
    return this.assignMealToSelf.get(support.getExecutionContext());
  }
}
