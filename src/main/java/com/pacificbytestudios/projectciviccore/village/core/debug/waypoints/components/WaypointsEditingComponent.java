package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.EnumMapCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;

public class WaypointsEditingComponent implements Component<EntityStore> {
  public static final BuilderCodec<WaypointsEditingComponent> CODEC = BuilderCodec
      .builder(WaypointsEditingComponent.class, WaypointsEditingComponent::new)

      .append(
          new KeyedCodec<>("StructureId", Codec.UUID_BINARY),
          (obj, value) -> obj.structureId = value,
          (obj) -> obj.structureId)
      .add()

      .append(new KeyedCodec<>("StructureWaypoints",
          new EnumMapCodec<>(
              MarkerType.class,
              new ArrayCodec<BlockTransform<Vec3i>>(
                  BlockTransform.VEC3I_CODEC,
                  WaypointsEditingComponent::createWaypointArray))),
          (obj, value) -> obj.structureWaypoints = value,
          (obj) -> obj.structureWaypoints)
      .add()

      .append(
          new KeyedCodec<>(
              "DirtyPositionWaypoints",
              new SetCodec<UUID, HashSet<UUID>>(Codec.UUID_BINARY, HashSet::new, false),
              false),
          (obj, value) -> obj.dirtyPositionWaypoints = value == null ? new HashSet<>() : value,
          (obj) -> obj.dirtyPositionWaypoints)
      .add()

      .build();

  private UUID structureId;
  private Map<MarkerType, BlockTransform<Vec3i>[]> structureWaypoints = new HashMap<>();
  private Set<UUID> dirtyPositionWaypoints = new HashSet<>();

  public record Waypoint(UUID waypointId, MarkerType type, BlockTransform<?> transform, boolean isAccess) {
  }

  private record WaypointCandidate(Waypoint waypoint, double distanceSquared) {
  }

  public UUID getStructureId() {
    return structureId;
  }

  public void setStructureId(UUID structureId) {
    this.structureId = structureId;
  }

  public Map<MarkerType, BlockTransform<Vec3i>[]> getStructureWaypoints() {
    return this.structureWaypoints;
  }

  public void addStructureWaypoint(MarkerType markerType, BlockTransform<Vec3i> waypoint) {
    this.structureWaypoints.compute(markerType, (key, waypoints) -> appendWaypoint(waypoints, waypoint));
  }

  public Waypoint getNearestWaypoint(Vec3d target, double threshold) {
    double thresholdSquared = threshold * threshold;
    WaypointCandidate nearest = null;

    for (Entry<MarkerType, BlockTransform<Vec3i>[]> entry : this.structureWaypoints.entrySet()) {
      nearest = nearestOf(
          nearest,
          getNearestWaypoint(entry.getKey(), entry.getValue(), target, thresholdSquared));
    }

    return nearest == null ? null : nearest.waypoint();
  }

  private WaypointCandidate getNearestWaypoint(
      MarkerType type,
      BlockTransform<Vec3i>[] waypoints,
      Vec3d target,
      double thresholdSquared) {
    WaypointCandidate nearest = null;
    for (BlockTransform<Vec3i> waypoint : waypoints) {
      nearest = nearestOf(nearest, waypointCandidate(type, waypoint, target, thresholdSquared, isAccessMarker(type)));
    }

    return nearest;
  }

  private static WaypointCandidate nearestOf(WaypointCandidate current, WaypointCandidate candidate) {
    if (candidate == null) {
      return current;
    }
    if (current == null || candidate.distanceSquared < current.distanceSquared) {
      return candidate;
    }
    return current;
  }

  private static WaypointCandidate waypointCandidate(
      MarkerType type,
      BlockTransform<Vec3i> waypoint,
      Vec3d target,
      double thresholdSquared,
      boolean isAccess) {
    if (isAccess && waypoint.getAccessOffset() == null) {
      return null;
    }

    double distanceSquared = target.distanceSquared(waypointPosition(waypoint, isAccess));
    if (distanceSquared > thresholdSquared) {
      return null;
    }

    UUID waypointId = isAccess ? waypoint.getAccessId() : waypoint.getPositionId();
    return new WaypointCandidate(
        new Waypoint(waypointId, type, waypoint, isAccess),
        distanceSquared);
  }

  public static Vec3d waypointPosition(BlockTransform<Vec3i> waypoint, boolean isAccess) {
    if (!isAccess) {
      return waypoint.getPosition().to3d();
    }
    return waypoint.getAccessOffset().to3d();
  }

  private static boolean isAccessMarker(MarkerType markerType) {
    return switch (markerType) {
      case BENCH_ACCESS, STORAGE_ACCESS -> true;
      default -> false;
    };
  }

  private static BlockTransform<Vec3i>[] appendWaypoint(
      BlockTransform<Vec3i>[] waypoints,
      BlockTransform<Vec3i> waypoint) {
    int waypointCount = waypoints == null ? 0 : waypoints.length;
    BlockTransform<Vec3i>[] updatedWaypoints = waypoints == null
        ? createWaypointArray(1)
        : Arrays.copyOf(waypoints, waypointCount + 1);

    updatedWaypoints[waypointCount] = waypoint;
    return updatedWaypoints;
  }

  public static ComponentType<EntityStore, WaypointsEditingComponent> getComponentType() {
    return ProjectCivicCore.get().getWaypointEditingComponentType();
  }

  @SuppressWarnings("unchecked")
  private static BlockTransform<Vec3i>[] createWaypointArray(int size) {
    return (BlockTransform<Vec3i>[]) new BlockTransform<?>[size];
  }

  @Override
  public WaypointsEditingComponent clone() {
    WaypointsEditingComponent component = new WaypointsEditingComponent();
    component.structureId = this.structureId;
    component.dirtyPositionWaypoints = new HashSet<>(this.dirtyPositionWaypoints);
    component.structureWaypoints = Utils.copyMapArray(this.structureWaypoints);
    return component;
  }
}
