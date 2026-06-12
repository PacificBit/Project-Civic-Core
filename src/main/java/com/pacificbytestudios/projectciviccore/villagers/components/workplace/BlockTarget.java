package com.pacificbytestudios.projectciviccore.villagers.components.workplace;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform3D;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

@SerializableClass
public class BlockTarget {
  public static final BuilderCodec<BlockTarget> CODEC = BlockTargetCodec.CODEC;

  Vec3i targetBlock;
  Vec3f accessPosition;

  public BlockTarget() {
  }

  public BlockTarget(Vec3i targetBlock) {
    this.targetBlock = targetBlock;
  }

  public BlockTarget(Vec3i targetBlock, Vec3f accessPosition) {
    this.targetBlock = targetBlock;
    this.accessPosition = accessPosition;
  }

  public BlockTarget(BlockTransform3D transform3d) {
    targetBlock = transform3d.getPosition3i();
    accessPosition = transform3d.getAccessOffset3f();
  }

  public Vec3i getTargetBlock() {
    return targetBlock;
  }

  public Vec3f getAccessPosition() {
    return accessPosition;
  }

  @Override
  public BlockTarget clone() {
    return BlockTargetCodec.clone(this);
  }
}
