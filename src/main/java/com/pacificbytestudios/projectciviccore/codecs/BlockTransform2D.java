package com.pacificbytestudios.projectciviccore.codecs;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D.Vec2i;
import com.pacificbytestudios.projectciviccore.interfaces.Copyable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SerializableClass
public class BlockTransform2D implements Copyable<BlockTransform2D> {
  public static final BuilderCodec<BlockTransform2D> CODEC = BlockTransform2DCodec.CODEC;

  UUID positionId;
  UUID accessId;

  @Setter(lombok.AccessLevel.NONE)
  Vec2i position;
  Rotation rotation;

  @Setter(lombok.AccessLevel.NONE)
  Vec2i accessOffset;

  public BlockTransform2D() {
  }

  public BlockTransform2D(@Nonnull Vec2i position) {
    this(UUID.randomUUID(), null, position, null, null);
  }

  public BlockTransform2D(
      @Nonnull Vec2i position,
      Rotation rotation) {
    this(UUID.randomUUID(), null, position, rotation, null);
  }

  public BlockTransform2D(
      @Nonnull Vec2i position,
      Rotation rotation,
      Vec2i accessOffset) {
    this(UUID.randomUUID(), accessOffset == null ? null : UUID.randomUUID(), position, rotation, accessOffset);
  }

  public BlockTransform2D(
      @Nonnull UUID positionId,
      UUID accessId,
      @Nonnull Vec2i position,
      Rotation rotation,
      Vec2i accessOffset) {
    this.positionId = positionId;
    this.accessId = accessId;
    this.position = Vec2i.from(position);
    this.rotation = rotation;
    this.accessOffset = Vec2i.from(accessOffset);
  }

  public UUID ensurePositionId() {
    if (this.positionId == null) {
      this.positionId = UUID.randomUUID();
    }
    return this.positionId;
  }

  public UUID ensureAccessId() {
    if (this.accessId == null) {
      this.accessId = UUID.randomUUID();
    }
    return this.accessId;
  }

  public void setPosition(Vec2i position) {
    this.position = Vec2i.from(position);
  }

  public void setAccessOffset(Vec2i accessOffset) {
    if (accessOffset != null && this.accessId == null) {
      this.accessId = UUID.randomUUID();
    }
    this.accessOffset = Vec2i.from(accessOffset);
  }

  public static BlockTransform2D from(Vec2i position) {
    return position == null ? null : new BlockTransform2D(position);
  }

  @Override
  public BlockTransform2D clone() {
    return BlockTransform2D.clone(this);
  }

  public static BlockTransform2D clone(BlockTransform2D o) {
    return BlockTransform2DCodec.clone(o);
  }
}
