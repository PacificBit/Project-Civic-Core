package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.utils.SpawnFunction;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;

public class SpawnWorkplaceTask extends AVillageTask {

  public static final BuilderCodec<SpawnWorkplaceTask> CODEC = BuilderCodec
      .builder(SpawnWorkplaceTask.class, SpawnWorkplaceTask::new)

      .append(
          new KeyedCodec<>("WorkplaceType", new EnumCodec<StructureType>(StructureType.class)),
          (obj, value) -> obj.workplaceType = value,
          (obj) -> obj.workplaceType)
      .add()

      .append(new KeyedCodec<>("Position", Vector3i.CODEC),
          (obj, value) -> obj.position = value,
          obj -> obj.position)
      .add()

      .append(new KeyedCodec<>("Rotation", Rotation.CODEC, false),
          (obj, value) -> obj.rotation = value,
          obj -> obj.rotation)
      .add()

      .build();

  StructureType workplaceType;
  Vector3i position;
  Rotation rotation = Rotation.None;

  public SpawnWorkplaceTask() {
  }

  public SpawnWorkplaceTask(StructureType workplaceType, Vector3i position) {
    this(workplaceType, position, Rotation.None);
  }

  public SpawnWorkplaceTask(StructureType workplaceType, Vector3i position, Rotation rotation) {
    this.workplaceType = workplaceType;
    this.position = position;
    this.rotation = rotation;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vector3i heartPos,
      VillageHeartComponent heart) {
    SpawnFunction spawnFunction = Utils.getSpawnFunction(this.workplaceType);
    spawnFunction.apply(chunk, this.position, this.rotation, heart);
  }
}
