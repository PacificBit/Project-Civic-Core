package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.utils.SpawnFunction;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceType;

public class SpawnWorkplaceTask extends AVillageTask {

  public static final BuilderCodec<SpawnWorkplaceTask> CODEC = BuilderCodec
      .builder(SpawnWorkplaceTask.class, SpawnWorkplaceTask::new)

      .append(
          new KeyedCodec<>("WorkplaceType", new EnumCodec<WorkplaceType>(WorkplaceType.class)),
          (obj, value) -> obj.workplaceType = value,
          (obj) -> obj.workplaceType)
      .add()

      .append(new KeyedCodec<>("Position", Vector3i.CODEC),
          (obj, value) -> obj.position = value,
          obj -> obj.position)
      .add()

      .build();

  WorkplaceType workplaceType;
  Vector3i position;

  public SpawnWorkplaceTask() {
  }

  public SpawnWorkplaceTask(WorkplaceType workplaceType, Vector3i position) {
    this.workplaceType = workplaceType;
    this.position = position;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vector3i heartPos,
      VillageHeartComponent heart) {
    SpawnFunction spawnFunction = Utils.getSpawnFunction(this.workplaceType);
    spawnFunction.apply(chunk, this.position);
  }
}
