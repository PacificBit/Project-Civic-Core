package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components;

import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.interfaces.Copyable;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudio.codecbutler.annotations.SerialWithCodec;

@SerializableClass
public class Waypoint implements Copyable<Waypoint> {
  public static final BuilderCodec<Waypoint> CODEC = WaypointCodec.CODEC;

  UUID waypointId;
  MarkerType type;
  @SerialWithCodec("com.pacificbytestudios.projectciviccore.codecs.BlockTransform.VEC3I_CODEC")
  BlockTransform<Vec3i> intTransform;
  @SerialWithCodec("com.pacificbytestudios.projectciviccore.codecs.BlockTransform.VEC3F_CODEC")
  BlockTransform<Vec3f> floatTransform;
  boolean isAccess;

  public Waypoint() {
  }

  public Waypoint(UUID waypointId, MarkerType type, BlockTransform<Vec3i> transform, boolean isAccess) {
    this.waypointId = waypointId;
    this.type = type;
    this.intTransform = transform;
    this.isAccess = isAccess;
  }

  public static Waypoint from(MarkerType type, BlockTransform<?> transform, boolean isAccess) {
    transform.ensurePositionId();
    UUID waypointId = isAccess ? transform.ensureAccessId() : transform.getPositionId();
    Object position = transform.getPosition();
    if (position instanceof Vec3i vec3i) {
      return new Waypoint(waypointId, type, cloneIntTransform(transform, vec3i), isAccess);
    }
    if (position instanceof Vec3f vec3f) {
      Waypoint waypoint = new Waypoint();
      waypoint.waypointId = waypointId;
      waypoint.type = type;
      waypoint.floatTransform = cloneFloatTransform(transform, vec3f);
      waypoint.isAccess = isAccess;
      return waypoint;
    }

    throw new IllegalArgumentException("Unsupported waypoint transform position type: " + position.getClass());
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

  public BlockTransform<?> getTransform() {
    if (this.intTransform != null) {
      return this.intTransform;
    }
    return this.floatTransform;
  }

  public BlockTransform<?> transform() {
    return this.getTransform();
  }

  public void setTransform(BlockTransform<Vec3i> transform) {
    this.intTransform = transform;
    this.floatTransform = null;
  }

  public void setFloatTransform(BlockTransform<Vec3f> transform) {
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
      this.intTransform.setPosition(position.to3i());
      return;
    }

    this.floatTransform.setPosition(position.to3f());
  }

  public void setAccessPosition(Vec3d position) {
    this.getTransform().setAccessOffset(position.to3f());
  }

  public Vec3d getWaypointPosition() {
    BlockTransform<?> transform = this.getTransform();
    if (this.isAccess) {
      return transform.getAccessOffset().to3d();
    }

    return positionTo3d(transform.getPosition());
  }

  @Override
  public Waypoint clone() {
    return WaypointCodec.clone(this);
  }

  private static Vec3d positionTo3d(Object position) {
    if (position instanceof Vec3i vec3i) {
      return vec3i.to3d();
    }
    if (position instanceof Vec3f vec3f) {
      return vec3f.to3d();
    }

    throw new IllegalArgumentException("Unsupported waypoint position type: " + position.getClass());
  }

  private static BlockTransform<Vec3i> cloneIntTransform(BlockTransform<?> transform, Vec3i position) {
    BlockTransform<Vec3i> cloned = new BlockTransform<>();
    cloned.setPositionId(transform.getPositionId());
    cloned.setAccessId(transform.getAccessId());
    cloned.setPosition(Vec3i.from(position));
    cloned.setRotation(transform.getRotation());
    cloned.setAccessOffset(Vec3f.from(transform.getAccessOffset()));
    return cloned;
  }

  private static BlockTransform<Vec3f> cloneFloatTransform(BlockTransform<?> transform, Vec3f position) {
    BlockTransform<Vec3f> cloned = new BlockTransform<>();
    cloned.setPositionId(transform.getPositionId());
    cloned.setAccessId(transform.getAccessId());
    cloned.setPosition(Vec3f.from(position));
    cloned.setRotation(transform.getRotation());
    cloned.setAccessOffset(Vec3f.from(transform.getAccessOffset()));
    return cloned;
  }
}
