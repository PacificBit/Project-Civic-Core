package com.pacificbytestudios.projectciviccore.codecs;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.interfaces.Copyable;

@SerializableClass
public class BlockTransform3D implements Copyable<BlockTransform3D> {
  public static final BuilderCodec<BlockTransform3D> CODEC = BlockTransform3DCodec.CODEC;

  UUID positionId;
  UUID accessId;
  Vec3d position;
  Rotation rotation;
  Vec3d accessOffset;

  public BlockTransform3D() {
  }

  public BlockTransform3D(@Nonnull Vec3i position) {
    this(Vec3d.from(position));
  }

  public BlockTransform3D(@Nonnull Vec3f position) {
    this(Vec3d.from(position));
  }

  public BlockTransform3D(@Nonnull Vec3d position) {
    this(UUID.randomUUID(), null, position, null, null);
  }

  public BlockTransform3D(
      @Nonnull Vec3i position,
      Rotation rotation) {
    this(Vec3d.from(position), rotation);
  }

  public BlockTransform3D(
      @Nonnull Vec3f position,
      Rotation rotation) {
    this(Vec3d.from(position), rotation);
  }

  public BlockTransform3D(
      @Nonnull Vec3d position,
      Rotation rotation) {
    this(UUID.randomUUID(), null, position, rotation, null);
  }

  public BlockTransform3D(
      @Nonnull Vec3i position,
      Rotation rotation,
      Vec3f accessOffset) {
    this(Vec3d.from(position), rotation, Vec3d.from(accessOffset));
  }

  public BlockTransform3D(
      @Nonnull Vec3f position,
      Rotation rotation,
      Vec3f accessOffset) {
    this(Vec3d.from(position), rotation, Vec3d.from(accessOffset));
  }

  public BlockTransform3D(
      @Nonnull Vec3d position,
      Rotation rotation,
      Vec3d accessOffset) {
    this(UUID.randomUUID(), accessOffset == null ? null : UUID.randomUUID(), position, rotation, accessOffset);
  }

  public BlockTransform3D(
      @Nonnull UUID positionId,
      UUID accessId,
      @Nonnull Vec3i position,
      Rotation rotation,
      Vec3f accessOffset) {
    this(positionId, accessId, Vec3d.from(position), rotation, Vec3d.from(accessOffset));
  }

  public BlockTransform3D(
      @Nonnull UUID positionId,
      UUID accessId,
      @Nonnull Vec3f position,
      Rotation rotation,
      Vec3f accessOffset) {
    this(positionId, accessId, Vec3d.from(position), rotation, Vec3d.from(accessOffset));
  }

  public BlockTransform3D(
      @Nonnull UUID positionId,
      UUID accessId,
      @Nonnull Vec3d position,
      Rotation rotation,
      Vec3d accessOffset) {
    this.positionId = positionId;
    this.accessId = accessId;
    this.position = position;
    this.rotation = rotation;
    this.accessOffset = accessOffset;
  }

  public UUID getPositionId() {
    return positionId;
  }

  public UUID ensurePositionId() {
    if (this.positionId == null) {
      this.positionId = UUID.randomUUID();
    }
    return this.positionId;
  }

  public void setPositionId(UUID positionId) {
    this.positionId = positionId;
  }

  public UUID getAccessId() {
    return accessId;
  }

  public UUID ensureAccessId() {
    if (this.accessId == null) {
      this.accessId = UUID.randomUUID();
    }
    return this.accessId;
  }

  public void setAccessId(UUID accessId) {
    this.accessId = accessId;
  }

  public Vec3d getPosition() {
    return position;
  }

  public Vec3i getPosition3i() {
    return this.position == null ? null : this.position.to3i();
  }

  public Vec3f getPosition3f() {
    return Vec3f.from(this.position);
  }

  public void setPosition(Vec3d position) {
    this.position = position;
  }

  public void setPosition(Vec3i position) {
    this.position = Vec3d.from(position);
  }

  public void setPosition(Vec3f position) {
    this.position = Vec3d.from(position);
  }

  public Rotation getRotation() {
    return rotation;
  }

  public void setRotation(Rotation rotation) {
    this.rotation = rotation;
  }

  public Vec3d getAccessOffset() {
    return accessOffset;
  }

  public Vec3f getAccessOffset3f() {
    return Vec3f.from(this.accessOffset);
  }

  public void setAccessOffset(Vec3d accessOffset) {
    if (accessOffset != null && this.accessId == null) {
      this.accessId = UUID.randomUUID();
    }
    this.accessOffset = accessOffset;
  }

  public void setAccessOffset(Vec3f accessOffset) {
    this.setAccessOffset(Vec3d.from(accessOffset));
  }

  public static BlockTransform3D from(Vec3i position) {
    return position == null ? null : new BlockTransform3D(position);
  }

  public static BlockTransform3D from(Vec3f position) {
    return position == null ? null : new BlockTransform3D(position);
  }

  public static BlockTransform3D from(Vec3d position) {
    return position == null ? null : new BlockTransform3D(position);
  }

  @Override
  public BlockTransform3D clone() {
    return BlockTransform3D.clone(this);
  }

  public static BlockTransform3D clone(BlockTransform3D o) {
    return BlockTransform3DCodec.clone(o);
  }

}
