package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components;

import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;

@SerializableClass
public class WaypointOperation {
  public static final BuilderCodec<WaypointOperation> CODEC = WaypointOperationCodec.CODEC;

  UUID waypointId;
  Operation operationType;
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

  public UUID getWaypointId() {
    return this.waypointId;
  }

  public void setWaypointId(UUID waypointId) {
    this.waypointId = waypointId;
  }

  public Operation getOperationType() {
    return this.operationType;
  }

  public void setOperationType(Operation operationType) {
    this.operationType = operationType;
  }

  public Waypoint getOldWaypoint() {
    return this.oldWaypoint;
  }

  public Waypoint getNewWaypoint() {
    return this.newWaypoint;
  }

  public void setNewWaypoint(Waypoint newWaypoint) {
    this.newWaypoint = newWaypoint;
  }

  @Override
  public WaypointOperation clone() {
    return WaypointOperationCodec.clone(this);
  }
}
