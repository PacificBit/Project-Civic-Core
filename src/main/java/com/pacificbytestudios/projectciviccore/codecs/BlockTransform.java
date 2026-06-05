package com.pacificbytestudios.projectciviccore.codecs;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.interfaces.Copyable;

public class BlockTransform implements Copyable<BlockTransform> {
  public static final BuilderCodec<BlockTransform> CODEC = BuilderCodec
      .builder(BlockTransform.class, BlockTransform::new)

      .append(new KeyedCodec<>("Id", Codec.UUID_BINARY),
          (obj, value) -> obj.id = value,
          (obj) -> obj.id)
      .add()

      .append(new KeyedCodec<>("Position", Vec3i.CODEC),
          (obj, value) -> obj.position = value,
          (obj) -> obj.position)
      .add()

      .append(new KeyedCodec<>("Rotation", Rotation.CODEC),
          (obj, value) -> obj.rotation = value,
          (obj) -> obj.rotation)
      .add()

      .append(new KeyedCodec<>("AccessOffset", Vec3f.CODEC),
          (obj, value) -> obj.accessOffset = value,
          (obj) -> obj.accessOffset)
      .add()

      .build();

  private UUID id;
  private Vec3i position;
  private Rotation rotation;
  private Vec3f accessOffset;

  public BlockTransform() {
  }

  public BlockTransform(
      @Nonnull Vec3i position,
      @Nonnull Rotation rotation,
      @Nonnull Vec3f accessOffset) {
    this(UUID.randomUUID(), position, rotation, accessOffset);
  }

  public BlockTransform(
      @Nonnull UUID id,
      @Nonnull Vec3i position,
      @Nonnull Rotation rotation,
      @Nonnull Vec3f accessOffset) {
    this.id = id;
    this.position = position;
    this.rotation = rotation;
    this.accessOffset = accessOffset;
  }

  public Vec3i getPosition() {
    return position;
  }

  public void setPosition(Vec3i position) {
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
    this.accessOffset = accessOffset;
  }

  public BlockTransform clone() {
    BlockTransform transform = new BlockTransform();
    transform.id = this.id;
    transform.position = this.position.clone();
    transform.rotation = this.rotation;
    transform.accessOffset = this.accessOffset.clone();
    return transform;
  }
}
