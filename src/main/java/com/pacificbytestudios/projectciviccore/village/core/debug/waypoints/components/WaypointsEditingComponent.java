package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public class WaypointsEditingComponent implements Component<EntityStore> {
  public static final BuilderCodec<WaypointsEditingComponent> CODEC = BuilderCodec
      .builder(WaypointsEditingComponent.class, WaypointsEditingComponent::new)

      .append(
          new KeyedCodec<>("StructureId", Codec.UUID_BINARY),
          (obj, value) -> obj.structureId = value,
          (obj) -> obj.structureId)
      .add()

      .append(
          new KeyedCodec<>(
              "DirtyWaypoints",
              new SetCodec<UUID, HashSet<UUID>>(Codec.UUID_BINARY, HashSet::new, false),
              false),
          (obj, value) -> obj.dirtyWaypoints = value == null ? new HashSet<>() : value,
          (obj) -> obj.dirtyWaypoints)
      .add()

      .build();

  private UUID structureId;
  private Set<UUID> dirtyWaypoints = new HashSet<>();

  @Override
  public WaypointsEditingComponent clone() {
    WaypointsEditingComponent component = new WaypointsEditingComponent();
    component.structureId = this.structureId;
    component.dirtyWaypoints = new HashSet<>(this.dirtyWaypoints);

    return component;
  }

  public static ComponentType<EntityStore, WaypointsEditingComponent> getComponentType() {
    return ProjectCivicCore.get().getWaypointEditingComponentType();
  }

  public UUID getStructureId() {
    return this.structureId;
  }

  public void setStructureId(UUID structureId) {
    this.structureId = structureId;
  }

  public Set<UUID> getDirtyWaypoints() {
    return Collections.unmodifiableSet(this.dirtyWaypoints);
  }

  public void setDirtyWaypoints(Set<UUID> dirtyWaypoints) {
    this.dirtyWaypoints = dirtyWaypoints == null ? new HashSet<>() : new HashSet<>(dirtyWaypoints);
  }

  public boolean addDirtyWaypoint(UUID waypointId) {
    return waypointId != null && this.dirtyWaypoints.add(waypointId);
  }

  public boolean removeDirtyWaypoint(UUID waypointId) {
    return waypointId != null && this.dirtyWaypoints.remove(waypointId);
  }

  public boolean isDirtyWaypoint(UUID waypointId) {
    return waypointId != null && this.dirtyWaypoints.contains(waypointId);
  }

  public void clearDirtyWaypoints() {
    this.dirtyWaypoints.clear();
  }
}
