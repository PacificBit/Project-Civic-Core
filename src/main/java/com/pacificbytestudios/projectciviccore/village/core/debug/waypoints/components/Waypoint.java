package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components;

import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform3D;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.interfaces.Copyable;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;

@SerializableClass
public class Waypoint implements Copyable<Waypoint> {
  public static final BuilderCodec<Waypoint> CODEC = WaypointCodec.CODEC;

  UUID waypointId;
  MarkerType type;
  BlockTransform3D intTransform;
  BlockTransform3D floatTransform;
  boolean isAccess;

  public Waypoint() {
  }

  public Waypoint(UUID waypointId, MarkerType type, BlockTransform3D transform, boolean isAccess) {
    this.waypointId = waypointId;
    this.type = type;
    this.intTransform = transform;
    this.isAccess = isAccess;
  }

  public static Waypoint from(MarkerType type, BlockTransform3D transform, boolean isAccess) {
    transform.ensurePositionId();
    UUID waypointId = isAccess ? transform.ensureAccessId() : transform.getPositionId();
    if (isIntegral(transform.getPosition())) {
      return new Waypoint(waypointId, type, BlockTransform3D.clone(transform), isAccess);
    }

    Waypoint waypoint = new Waypoint();
    waypoint.waypointId = waypointId;
    waypoint.type = type;
    waypoint.floatTransform = BlockTransform3D.clone(transform);
    waypoint.isAccess = isAccess;
    return waypoint;
  }

  public UUID getWaypointId() {
    return this.waypointId;
  }

  public UUID waypointId() {
    return this.waypointId;
  }

  public void setWaypointId(UUID waypointId) {
    this.waypointId = waypointId;
  }

  public MarkerType getType() {
    return this.type;
  }

  public MarkerType type() {
    return this.type;
  }

  public void setType(MarkerType type) {
    this.type = type;
  }

  public BlockTransform3D getTransform() {
    if (this.intTransform != null) {
      return this.intTransform;
    }
    return this.floatTransform;
  }

  public BlockTransform3D transform() {
    return this.getTransform();
  }

  public void setTransform(BlockTransform3D transform) {
    this.intTransform = transform;
    this.floatTransform = null;
  }

  public void setFloatTransform(BlockTransform3D transform) {
    this.intTransform = null;
    this.floatTransform = transform;
  }

  public boolean isFloatTransform() {
    return this.floatTransform != null;
  }

  public boolean isAccess() {
    return this.isAccess;
  }

  public void setAccess(boolean isAccess) {
    this.isAccess = isAccess;
  }

  public UUID getPositionId() {
    return this.getTransform().getPositionId();
  }

  public UUID getAccessId() {
    return this.getTransform().getAccessId();
  }

  public void setPosition(Vec3d position) {
    if (this.intTransform != null) {
      this.intTransform.setPosition(position);
      return;
    }

    this.floatTransform.setPosition(position);
  }

  public void setAccessPosition(Vec3d position) {
    this.getTransform().setAccessOffset(position);
  }

  public Vec3d getWaypointPosition() {
    BlockTransform3D transform = this.getTransform();
    if (this.isAccess) {
      return Vec3d.from(transform.getAccessOffset());
    }

    return Vec3d.from(transform.getPosition());
  }

  @Override
  public Waypoint clone() {
    return WaypointCodec.clone(this);
  }

  private static boolean isIntegral(Vec3d position) {
    return position == null
        || position.x == Math.floor(position.x)
            && position.y == Math.floor(position.y)
            && position.z == Math.floor(position.z);
  }
}
