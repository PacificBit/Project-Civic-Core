package com.pacificbytestudios.projectciviccore.codecs;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

public class EntityRef {
  public static final BuilderCodec<EntityRef> CODEC = BuilderCodec
      .builder(EntityRef.class, EntityRef::new)

      .append(new KeyedCodec<>("WorldName", Codec.STRING),
          (obj, value) -> obj.worldName = value,
          (obj) -> obj.worldName)
      .add()

      .append(new KeyedCodec<>("Position", Vec3i.CODEC),
          (obj, value) -> obj.position = value,
          (obj) -> obj.position)
      .add()

      .build();

  private String worldName;
  private Vec3i position;
  private Ref<ChunkStore> entityRef;

  public EntityRef() {
    this(null, null);
  }

  public EntityRef(String worldName, Vec3i position) {
    this.worldName = worldName;
    this.position = position;
  }

  public Ref<ChunkStore> getRef() {
    if (this.entityRef == null) {
      World world = Universe.get().getWorld(worldName);
      Vec3i villageHeartPos = position;
      WorldChunk chunk = world.getChunk(
          ChunkUtil.indexChunkFromBlock(villageHeartPos.x, villageHeartPos.z));

      if (chunk != null) {
        this.entityRef = chunk.getBlockComponentEntity(villageHeartPos.x, villageHeartPos.y, villageHeartPos.z);
      }
    }
    return this.entityRef;
  }

  public Vec3i getPosition() {
    return this.position;
  }

  public String getWorldName() {
    return this.worldName;
  }

  public EntityRef clone() {
    EntityRef newLocation = new EntityRef();
    newLocation.worldName = this.worldName;
    newLocation.position = this.position;
    newLocation.entityRef = this.entityRef;
    return newLocation;
  }

  @Override
  public String toString() {
    return "EntityRef: { WorldName: %s, Position: %s }"
        .formatted(this.worldName, this.position.toString());
  }
}
