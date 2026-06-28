package com.pacificbytestudios.projectciviccore.codecs;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import lombok.Getter;

@Getter
@SerializableClass
public class BlockContext {
  public static final BuilderCodec<BlockContext> CODEC = BlockContextCodec.CODEC;

  String worldName;
  Long chunkIndex;
  Vec3i position;

  public BlockContext() {
    this(null, null, null);
  }

  public BlockContext(String worldName, Long chunkIndex, Vec3i position) {
    this.worldName = worldName;
    this.position = position;
    this.chunkIndex = chunkIndex;
  }

  public World getWorld() {
    return Universe.get().getWorld(worldName);
  }

  public BlockContext clone() {
    return BlockContextCodec.clone(this);
  }

  @Override
  public String toString() {
    return "BlockContext: { WorldName: %s, ChunkIndex: %d, Position: %s }"
        .formatted(this.worldName, this.chunkIndex, this.position.toString());
  }
}
