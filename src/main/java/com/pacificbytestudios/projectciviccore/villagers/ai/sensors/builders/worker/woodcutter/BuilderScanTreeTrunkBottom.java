package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.woodcutter;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.Feature;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.woodcutter.ScanTreeTrunkBottom;

public class BuilderScanTreeTrunkBottom extends BuilderSensorBase {

  @Override
  public Sensor build(BuilderSupport support) {
    return new ScanTreeTrunkBottom(this);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Returns the next trunk to cut";
  }

  @Override
  public String getShortDescription() {
    return "Returns the next trunk to cut";
  }

  @Override
  public Builder<Sensor> readConfig(JsonElement data) {
    this.provideFeature(Feature.Position);
    return this;
  }

}
