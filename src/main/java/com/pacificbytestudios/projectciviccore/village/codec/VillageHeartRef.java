package com.pacificbytestudios.projectciviccore.village.codec;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class VillageHeartRef {
  public static final BuilderCodec<VillageHeartRef> CODEC = BuilderCodec
      .builder(VillageHeartRef.class, VillageHeartRef::new)

      .append(new KeyedCodec<>("WorldName", Codec.STRING),
          (obj, value) -> obj.worldName = value,
          (obj) -> obj.worldName)
      .add()

      .append(new KeyedCodec<>("Position", Vector3i.CODEC),
          (obj, value) -> obj.position = value,
          (obj) -> obj.position)
      .add()

      .build();

  private String worldName;
  private Vector3i position;
  private Ref<ChunkStore> villageHeartRef;

  public VillageHeartRef() {
    this(null, null);
  }

  public VillageHeartRef(String worldName, Vector3i position) {
    this.worldName = worldName;
    this.position = position;
  }

  public Ref<ChunkStore> getRef() {
    if (this.villageHeartRef == null) {
      World world = Universe.get().getWorld(worldName);
      Vector3i villageHeartPos = position;
      WorldChunk chunk = world.getChunkIfInMemory(
          ChunkUtil.indexChunkFromBlock(villageHeartPos.x, villageHeartPos.z));

      if (chunk != null) {
        this.villageHeartRef = chunk.getBlockComponentEntity(villageHeartPos.x, villageHeartPos.y, villageHeartPos.z);
      }
    }
    return this.villageHeartRef;
  }

  public VillageHeartRef clone() {
    VillageHeartRef newLocation = new VillageHeartRef();
    newLocation.worldName = this.worldName;
    newLocation.position = this.position;
    newLocation.villageHeartRef = this.villageHeartRef;
    return newLocation;
  }

  @Override
  public String toString() {
    return "VillageHeartLocation[" + this.worldName + " | pos=" + this.position + "]";
  }
}
