package com.pacificbytestudios.projectciviccore.villagers.components.workplace;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform3D;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import lombok.Getter;

@Getter
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

  @Override
  public BlockTarget clone() {
    return BlockTargetCodec.clone(this);
  }
}
