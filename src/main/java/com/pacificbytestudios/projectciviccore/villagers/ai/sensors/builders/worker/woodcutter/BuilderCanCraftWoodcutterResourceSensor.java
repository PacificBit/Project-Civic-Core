package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.woodcutter;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.holder.EnumHolder;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.woodcutter.CanCraftWoodcutterResourceSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.woodcutter.CanCraftWoodcutterResourceSensor.ResourceType;

public class BuilderCanCraftWoodcutterResourceSensor extends BuilderSensorBase {
  private final EnumHolder<ResourceType> resourceType = new EnumHolder<>();

  @Override
  public Sensor build(BuilderSupport support) {
    return new CanCraftWoodcutterResourceSensor(this, support);
  }

  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Experimental;
  }

  @Override
  public String getLongDescription() {
    return "Checks whether the wood cutter can craft the selected resource right now";
  }

  @Override
  public String getShortDescription() {
    return "Checks wood cutter craftability";
  }

  @Override
  public Builder<Sensor> readConfig(JsonElement data) {
    this.requireEnum(data, "ResourceType", this.resourceType, ResourceType.class,
        BuilderDescriptorState.Experimental,
        "Wood cutter resource to validate craftability for", null);
    return this;
  }

  public ResourceType getResourceType(BuilderSupport support) {
    return this.resourceType.get(support.getExecutionContext());
  }
}
