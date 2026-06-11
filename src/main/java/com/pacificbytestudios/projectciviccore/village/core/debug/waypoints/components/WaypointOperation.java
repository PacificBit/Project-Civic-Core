package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components;

import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

public class WaypointOperation {
  public static final BuilderCodec<WaypointOperation> CODEC = BuilderCodec
      .builder(WaypointOperation.class, WaypointOperation::new)

      .append(new KeyedCodec<>("WaypointId", Codec.UUID_BINARY),
          (obj, value) -> obj.waypointId = value,
          (obj) -> obj.waypointId)
      .add()

      .append(new KeyedCodec<>("OperationType", new EnumCodec<>(Operation.class), false),
          (obj, value) -> obj.operationType = value,
          (obj) -> obj.operationType)
      .add()

      .append(new KeyedCodec<>("OldWaypoint", Waypoint.CODEC, false),
          (obj, value) -> obj.oldWaypoint = value,
          (obj) -> obj.oldWaypoint)
      .add()

      .append(new KeyedCodec<>("NewWaypoint", Waypoint.CODEC, false),
          (obj, value) -> obj.newWaypoint = value,
          (obj) -> obj.newWaypoint)
      .add()

      .build();

  private UUID waypointId;
  private Operation operationType;
  private Waypoint oldWaypoint;
  private Waypoint newWaypoint;

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

  public Operation operationType() {
    return this.operationType;
  }

  public void setOperationType(Operation operationType) {
    this.operationType = operationType;
  }

  public Waypoint getOldWaypoint() {
    return this.oldWaypoint;
  }

  public void setOldWaypoint(Waypoint oldWaypoint) {
    this.oldWaypoint = oldWaypoint;
  }

  public Waypoint getNewWaypoint() {
    return this.newWaypoint;
  }

  public void setNewWaypoint(Waypoint newWaypoint) {
    this.newWaypoint = newWaypoint;
  }

  @Override
  public WaypointOperation clone() {
    WaypointOperation operation = new WaypointOperation();
    operation.waypointId = this.waypointId;
    operation.operationType = this.operationType;
    operation.oldWaypoint = this.oldWaypoint == null ? null : this.oldWaypoint.clone();
    operation.newWaypoint = this.newWaypoint == null ? null : this.newWaypoint.clone();
    return operation;
  }
}
