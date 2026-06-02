package com.pacificbytestudios.projectciviccore.codecs;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

public class BlockContext {
  public static final BuilderCodec<BlockContext> CODEC = BuilderCodec
      .builder(BlockContext.class, BlockContext::new)

      .append(new KeyedCodec<>("WorldName", Codec.STRING),
          (obj, value) -> obj.worldName = value,
          (obj) -> obj.worldName)
      .add()

      .append(new KeyedCodec<>("ChunkIndex", Codec.LONG),
          (obj, value) -> obj.chunkIndex = value,
          (obj) -> obj.chunkIndex)
      .add()

      .append(new KeyedCodec<>("Position", Vec3i.CODEC),
          (obj, value) -> obj.position = value,
          (obj) -> obj.position)
      .add()

      .build();

  private String worldName;
  private Long chunkIndex;
  private Vec3i position;

  public BlockContext() {
    this(null, null, null);
  }

  public BlockContext(String worldName, Long chunkIndex, Vec3i position) {
    this.worldName = worldName;
    this.position = position;
    this.chunkIndex = chunkIndex;
  }

  public String getWorldName() {
    return worldName;
  }

  public World getWorld() {
    return Universe.get().getWorld(worldName);
  }

  public Vec3i getPosition() {
    return this.position;
  }

  public Long getChunkIndex() {
    return chunkIndex;
  }

  public BlockContext clone() {
    BlockContext newLocation = new BlockContext();
    newLocation.worldName = this.worldName;
    newLocation.chunkIndex = this.chunkIndex;
    newLocation.position = Vec3i.from(this.position);
    return newLocation;
  }

  @Override
  public String toString() {
    return "BlockContext: { WorldName: %s, ChunkIndex: %d, Position: %s }"
        .formatted(this.worldName, this.chunkIndex, this.position.toString());
  }
}
