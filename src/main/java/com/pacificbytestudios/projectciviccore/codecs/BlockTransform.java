package com.pacificbytestudios.projectciviccore.codecs;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D.Vec2i;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.interfaces.Copyable;

public class BlockTransform<T> implements Copyable<BlockTransform<T>> {
  public static final BuilderCodec<BlockTransform<Vec2i>> VEC2I_CODEC = codec(Vec2i.CODEC);
  public static final BuilderCodec<BlockTransform<Vec3i>> VEC3I_CODEC = codec(Vec3i.CODEC);
  public static final BuilderCodec<BlockTransform<Vec3f>> VEC3F_CODEC = codec(Vec3f.CODEC);
  public static final BuilderCodec<BlockTransform<Vec3d>> VEC3D_CODEC = codec(Vec3d.CODEC);
  public static final BuilderCodec<BlockTransform<Vec3i>> CODEC = VEC3I_CODEC;

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static <T> BuilderCodec<BlockTransform<T>> codec(Codec<T> positionCodec) {
    return BuilderCodec
        .builder((Class<BlockTransform<T>>) (Class) BlockTransform.class, BlockTransform::new)

        .append(new KeyedCodec<>("PositionId", Codec.UUID_BINARY, false),
            (obj, value) -> obj.positionId = value,
            (obj) -> obj.positionId)
        .add()

        .append(new KeyedCodec<>("Position", positionCodec),
            (obj, value) -> obj.position = value,
            (obj) -> obj.position)
        .add()

        .append(new KeyedCodec<>("Rotation", Rotation.CODEC, false),
            (obj, value) -> obj.rotation = value,
            (obj) -> obj.rotation)
        .add()

        .append(new KeyedCodec<>("AccessOffset", Vec3f.CODEC, false),
            (obj, value) -> obj.accessOffset = value,
            (obj) -> obj.accessOffset)
        .add()

        .append(new KeyedCodec<>("AccessId", Codec.UUID_BINARY, false),
            (obj, value) -> obj.accessId = value,
            (obj) -> obj.accessId)
        .add()

        .build();
  }

  private UUID positionId;
  private UUID accessId;
  private T position;
  private Rotation rotation;
  private Vec3f accessOffset;

  public BlockTransform() {
  }

  public BlockTransform(@Nonnull T position) {
    this(UUID.randomUUID(), null, position, null, null);
  }

  public BlockTransform(
      @Nonnull T position,
      Rotation rotation) {
    this(UUID.randomUUID(), null, position, rotation, null);
  }

  public BlockTransform(
      @Nonnull T position,
      Rotation rotation,
      Vec3f accessOffset) {
    this(UUID.randomUUID(), accessOffset == null ? null : UUID.randomUUID(), position, rotation, accessOffset);
  }

  public BlockTransform(
      @Nonnull UUID positionId,
      UUID accessId,
      @Nonnull T position,
      Rotation rotation,
      Vec3f accessOffset) {
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

  public T getPosition() {
    return position;
  }

  public void setPosition(T position) {
    this.position = position;
  }

  public Rotation getRotation() {
    return rotation;
  }

  public void setRotation(Rotation rotation) {
    this.rotation = rotation;
  }

  public Vec3f getAccessOffset() {
    return accessOffset;
  }

  public void setAccessOffset(Vec3f accessOffset) {
    if (accessOffset != null && this.accessId == null) {
      this.accessId = UUID.randomUUID();
    }
    this.accessOffset = accessOffset;
  }

  public static <T> BlockTransform<T> from(T position) {
    if (position == null) {
      return null;
    }
    return new BlockTransform<>(position);
  }

  @Override
  public BlockTransform<T> clone() {
    return BlockTransform.clone(this);
  }

  public static <T> BlockTransform<T> clone(BlockTransform<T> o) {
    if (o == null) {
      return null;
    }

    BlockTransform<T> transform = new BlockTransform<>();
    transform.positionId = o.positionId;
    transform.accessId = o.accessId;
    transform.position = copyPosition(o.position);
    transform.rotation = o.rotation;
    transform.accessOffset = Vec3f.from(o.accessOffset);
    return transform;
  }

  @SuppressWarnings("unchecked")
  private static <T> T copyPosition(T position) {
    if (position instanceof Vec2i vec2i) {
      return (T) Vec2i.from(vec2i);
    }
    if (position instanceof Vec3i vec3i) {
      return (T) Vec3i.from(vec3i);
    }
    if (position instanceof Vec3f vec3f) {
      return (T) Vec3f.from(vec3f);
    }
    if (position instanceof Vec3d vec3d) {
      return (T) Vec3d.from(vec3d);
    }
    return position;
  }
}
