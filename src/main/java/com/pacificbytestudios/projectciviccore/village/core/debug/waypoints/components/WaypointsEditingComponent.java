package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components;

import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public class WaypointsEditingComponent implements Component<EntityStore> {
  public static final BuilderCodec<WaypointsEditingComponent> CODEC = BuilderCodec
      .builder(WaypointsEditingComponent.class, WaypointsEditingComponent::new)

      .append(
          new KeyedCodec<>("Uuid", Codec.UUID_BINARY),
          (obj, value) -> obj.structureId = value,
          (obj) -> obj.structureId)
      .add()

      .build();

  private UUID structureId;

  @Override
  public WaypointsEditingComponent clone() {
    WaypointsEditingComponent component = new WaypointsEditingComponent();
    component.structureId = this.structureId;

    return component;
  }

  public static ComponentType<EntityStore, WaypointsEditingComponent> getComponentType() {
    return ProjectCivicCore.get().getWaypointEditingComponentType();
  }
}
