package com.pacificbytestudios.projectciviccore.codecs;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudio.codecbutler.annotations.SerialIgnore;
import lombok.Getter;

@Getter
@SerializableClass
public class EntityRef {
  public static final BuilderCodec<EntityRef> CODEC = EntityRefCodec.CODEC;

  String worldName;
  Vec3i position;

  @SerialIgnore
  @Getter(lombok.AccessLevel.NONE)
  private Ref<ChunkStore> entityRef;

  public EntityRef() {
    this(null, null);
  }

  public EntityRef(String worldName, Vec3i position) {
    this.worldName = worldName;
    this.position = position;
  }

  public Ref<ChunkStore> getRef() {
    if (this.entityRef == null || !this.entityRef.isValid()) {
      World world = Universe.get().getWorld(worldName);
      Vec3i villageHeartPos = position;
      this.entityRef = Utils.getBlockComponentEntity(world, villageHeartPos);
    }
    return this.entityRef;
  }

  public EntityRef clone() {
    return EntityRefCodec.clone(this);
  }

  @Override
  public String toString() {
    return "EntityRef: { WorldName: %s, Position: %s }"
        .formatted(this.worldName, this.position.toString());
  }
}
