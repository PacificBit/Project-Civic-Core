package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.core.spawner.HouseSpawner;

public class SpawnHouseTask extends AVillageTask {

  public static final BuilderCodec<SpawnHouseTask> CODEC = BuilderCodec
      .builder(SpawnHouseTask.class, SpawnHouseTask::new)

      .append(new KeyedCodec<>("Position", Vector3i.CODEC),
          (obj, value) -> obj.position = value,
          obj -> obj.position)
      .add()

      .append(new KeyedCodec<>("Rotation", Rotation.CODEC, false),
          (obj, value) -> obj.rotation = value,
          obj -> obj.rotation)
      .add()

      .build();

  Vector3i position;
  Rotation rotation = Rotation.None;

  public SpawnHouseTask() {
  }

  public SpawnHouseTask(Vector3i position) {
    this(position, Rotation.None);
  }

  public SpawnHouseTask(Vector3i position, Rotation rotation) {
    this.position = position;
    this.rotation = rotation;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vector3i heartPos,
      VillageHeartComponent heart) {
    HouseSpawner.spawn(chunk, this.position, this.rotation, heart);
  }
}
