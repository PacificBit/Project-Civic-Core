package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components;

import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SerializableClass
public class WaypointOperation {
  public static final BuilderCodec<WaypointOperation> CODEC = WaypointOperationCodec.CODEC;

  UUID waypointId;
  Operation operationType;
  @Setter(lombok.AccessLevel.NONE)
  Waypoint oldWaypoint;
  Waypoint newWaypoint;

  public enum Operation {
    ADD, MOVE, DELETE
  }

  public WaypointOperation() {
  }

  public WaypointOperation(UUID waypointId, Operation operationType, Waypoint oldWaypoint, Waypoint newWaypoint) {
    this.waypointId = waypointId;
    this.operationType = operationType;
    this.oldWaypoint = oldWaypoint;
    this.newWaypoint = newWaypoint;
  }

  public WaypointOperation(Operation operationType, Waypoint oldWaypoint, Waypoint newWaypoint) {
    this(oldWaypoint == null ? null : oldWaypoint.getWaypointId(), operationType, oldWaypoint, newWaypoint);
  }

  @Override
  public WaypointOperation clone() {
    return WaypointOperationCodec.clone(this);
  }
}
