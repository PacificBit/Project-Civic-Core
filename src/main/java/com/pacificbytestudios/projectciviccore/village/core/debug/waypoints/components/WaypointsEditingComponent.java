package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform3D;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;
import lombok.Getter;
import lombok.Setter;

@SerializableClass
public class WaypointsEditingComponent implements Component<EntityStore> {
  public static final BuilderCodec<WaypointsEditingComponent> CODEC = WaypointsEditingComponentCodec.CODEC;

  @Getter
  @Setter
  UUID structureId;
  @Getter
  Map<MarkerType, Waypoint[]> structureWaypoints = new HashMap<>();
  @Getter
  @Setter
  WaypointOperation currentOperation;
  Stack<WaypointOperation> operations = new Stack<>();

  private record WaypointCandidate(Waypoint waypoint, double distanceSquared) {
  }

  public record WaypointChange(
      UUID waypointId,
      MarkerType markerType,
      WaypointOperation.Operation operation,
      Waypoint original,
      Waypoint current) {
  }

  public void addOperation(WaypointOperation operations) {
    this.operations.add(operations);
  }

  public void addStructureWaypoint(MarkerType markerType, BlockTransform3D transform) {
    this.addStructureWaypoint(Waypoint.from(markerType, transform, isAccessMarker(markerType)));
  }

  public void addStructureWaypoint(Waypoint waypoint) {
    this.structureWaypoints.compute(waypoint.getType(), (key, waypoints) -> appendWaypoint(waypoints, waypoint));
  }

  public void updateStructureWaypoint(MarkerType markerType, UUID uuid, Waypoint updated) {
    Waypoint[] waypoints = this.structureWaypoints.get(markerType);
    if (waypoints == null || waypoints.length == 0) {
      waypoints = createWaypointArray(1);
      waypoints[0] = updated;
    } else {
      for (int i = 0; i < waypoints.length; i++) {
        if (isTransformWithId(waypoints[i], uuid)) {
          waypoints[i] = updated;
          this.structureWaypoints.put(markerType, waypoints);
          return;
        }
      }

      waypoints = appendWaypoint(waypoints, updated);
    }
    this.structureWaypoints.put(markerType, waypoints);
  }

  public void removeStructureWaypoint(MarkerType markerType, UUID uuid) {
    Waypoint[] waypoints = this.structureWaypoints.get(markerType);
    if (waypoints == null || uuid == null) {
      return;
    }

    Waypoint[] remaining = Arrays.stream(waypoints)
        .filter(waypoint -> waypoint != null && !isTransformWithId(waypoint, uuid))
        .toArray(WaypointsEditingComponent::createWaypointArray);

    if (remaining.length == 0) {
      this.structureWaypoints.remove(markerType);
      return;
    }
    this.structureWaypoints.put(markerType, remaining);
  }

  public boolean hasPendingChanges() {
    if (!this.operations.isEmpty()) {
      return true;
    }
    return this.currentOperation != null && this.currentOperation.getNewWaypoint() != null;
  }

  /**
   * Moves a staged operation (selected waypoint that has actually been moved)
   * into the
   * operation history so it shows up as a pending change. Clears the staging
   * slot.
   */
  public void commitCurrentOperation() {
    WaypointOperation staged = this.currentOperation;
    this.currentOperation = null;
    if (staged == null || staged.getNewWaypoint() == null) {
      return;
    }

    if (staged.getOperationType() == null) {
      staged.setOperationType(WaypointOperation.Operation.MOVE);
    }
    if (staged.getWaypointId() == null && staged.getOldWaypoint() != null) {
      staged.setWaypointId(staged.getOldWaypoint().getWaypointId());
    }
    this.operations.push(staged);
  }

  public void clearOperations() {
    this.operations.clear();
    this.currentOperation = null;
  }

  /**
   * Collapses the operation history into one net change per waypoint, in the
   * order the
   * waypoints were first touched. Waypoints that were added and then deleted are
   * omitted.
   */
  public List<WaypointChange> getNetChanges() {
    Map<UUID, WaypointChange> changes = new LinkedHashMap<>();

    List<WaypointOperation> allOperations = new ArrayList<>(this.operations);
    if (this.currentOperation != null && this.currentOperation.getNewWaypoint() != null) {
      allOperations.add(this.currentOperation);
    }

    for (WaypointOperation operation : allOperations) {
      UUID waypointId = operationWaypointId(operation);
      if (waypointId == null) {
        continue;
      }

      WaypointChange previous = changes.get(waypointId);
      Waypoint original = previous == null ? operation.getOldWaypoint() : previous.original();
      Waypoint current = operation.getOperationType() == WaypointOperation.Operation.DELETE
          ? null
          : operation.getNewWaypoint();

      if (original == null && current == null) {
        changes.remove(waypointId);
        continue;
      }

      MarkerType markerType = current != null ? current.getType() : original.getType();
      changes.put(waypointId, new WaypointChange(
          waypointId,
          markerType,
          netOperation(original, current),
          original,
          current));
    }

    return new ArrayList<>(changes.values());
  }

  public void rollbackAll() {
    this.commitCurrentOperation();
    while (!this.operations.isEmpty()) {
      undoOperation(this.operations.pop());
    }
  }

  public boolean rollbackWaypoint(UUID waypointId) {
    if (waypointId == null) {
      return false;
    }
    this.commitCurrentOperation();

    List<WaypointOperation> affected = new ArrayList<>();
    for (Iterator<WaypointOperation> iterator = this.operations.iterator(); iterator.hasNext();) {
      WaypointOperation operation = iterator.next();
      if (waypointId.equals(operationWaypointId(operation))) {
        affected.add(operation);
        iterator.remove();
      }
    }

    for (int i = affected.size() - 1; i >= 0; i--) {
      undoOperation(affected.get(i));
    }

    return !affected.isEmpty();
  }

  private void undoOperation(WaypointOperation operation) {
    Waypoint oldWaypoint = operation.getOldWaypoint();
    Waypoint newWaypoint = operation.getNewWaypoint();

    if (operation.getOperationType() == WaypointOperation.Operation.ADD) {
      if (newWaypoint != null) {
        removeStructureWaypoint(newWaypoint.getType(), newWaypoint.getWaypointId());
      }
      return;
    }

    if (operation.getOperationType() == WaypointOperation.Operation.DELETE) {
      if (oldWaypoint != null) {
        addStructureWaypoint(oldWaypoint.clone());
      }
      return;
    }

    if (oldWaypoint != null) {
      updateStructureWaypoint(oldWaypoint.getType(), operationWaypointId(operation), oldWaypoint.clone());
    }
  }

  private static UUID operationWaypointId(WaypointOperation operation) {
    if (operation.getWaypointId() != null) {
      return operation.getWaypointId();
    }
    if (operation.getOldWaypoint() != null) {
      return operation.getOldWaypoint().getWaypointId();
    }
    if (operation.getNewWaypoint() != null) {
      return operation.getNewWaypoint().getWaypointId();
    }
    return null;
  }

  private static WaypointOperation.Operation netOperation(Waypoint original, Waypoint current) {
    if (original == null) {
      return WaypointOperation.Operation.ADD;
    }
    if (current == null) {
      return WaypointOperation.Operation.DELETE;
    }
    return WaypointOperation.Operation.MOVE;
  }

  public Waypoint getNearestWaypoint(Vec3d target, double threshold) {
    double thresholdSquared = threshold * threshold;
    WaypointCandidate nearest = null;

    for (Entry<MarkerType, Waypoint[]> entry : this.structureWaypoints.entrySet()) {
      nearest = nearestOf(
          nearest,
          getNearestWaypoint(entry.getValue(), target, thresholdSquared));
    }

    return nearest == null ? null : nearest.waypoint();
  }

  private WaypointCandidate getNearestWaypoint(
      Waypoint[] waypoints,
      Vec3d target,
      double thresholdSquared) {
    WaypointCandidate nearest = null;
    for (Waypoint waypoint : waypoints) {
      nearest = nearestOf(nearest, waypointCandidate(waypoint, target, thresholdSquared));
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
      Waypoint waypoint,
      Vec3d target,
      double thresholdSquared) {
    double distanceSquared = target.distanceSquared(waypoint.getWaypointPosition());
    if (distanceSquared > thresholdSquared) {
      return null;
    }

    return new WaypointCandidate(waypoint, distanceSquared);
  }

  private static boolean isAccessMarker(MarkerType markerType) {
    return switch (markerType) {
      case BENCH_ACCESS, STORAGE_ACCESS -> true;
      default -> false;
    };
  }

  private static Waypoint[] appendWaypoint(
      Waypoint[] waypoints,
      Waypoint waypoint) {
    int waypointCount = waypoints == null ? 0 : waypoints.length;
    Waypoint[] updatedWaypoints = waypoints == null
        ? createWaypointArray(1)
        : Arrays.copyOf(waypoints, waypointCount + 1);

    updatedWaypoints[waypointCount] = waypoint;
    return updatedWaypoints;
  }

  private static boolean isTransformWithId(Waypoint waypoint, UUID uuid) {
    return uuid.equals(waypoint.getPositionId()) || uuid.equals(waypoint.getAccessId());
  }

  public static ComponentType<EntityStore, WaypointsEditingComponent> getComponentType() {
    return ProjectCivicCore.get().getWaypointEditingComponentType();
  }

  private static Waypoint[] createWaypointArray(int size) {
    return new Waypoint[size];
  }

  @Override
  public WaypointsEditingComponent clone() {
    return WaypointsEditingComponentCodec.clone(this);
  }
}
