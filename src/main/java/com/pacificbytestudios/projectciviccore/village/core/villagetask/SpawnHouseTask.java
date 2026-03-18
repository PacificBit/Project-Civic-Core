package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
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

      .build();

  Vector3i position;

  public SpawnHouseTask() {
  }

  public SpawnHouseTask(Vector3i position) {
    this.position = position;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vector3i heartPos,
      VillageHeartComponent heart) {
    HouseSpawner.spawn(chunk, this.position);
  }
}
