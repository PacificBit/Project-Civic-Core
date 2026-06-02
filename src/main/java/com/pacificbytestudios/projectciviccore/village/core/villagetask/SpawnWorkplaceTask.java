package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.UUID;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.utils.SpawnerFunctions.BeginNewConstruction;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.enums.ConstructionMode;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class SpawnWorkplaceTask extends AVillageTask {

  public static final BuilderCodec<SpawnWorkplaceTask> CODEC = BuilderCodec
      .builder(SpawnWorkplaceTask.class, SpawnWorkplaceTask::new)

      .append(new KeyedCodec<>("StructureId", Codec.UUID_BINARY, false),
          (obj, value) -> obj.structureId = value,
          obj -> obj.structureId)
      .add()

      .append(
          new KeyedCodec<>("WorkplaceType", new EnumCodec<StructureType>(StructureType.class)),
          (obj, value) -> obj.workplaceType = value,
          (obj) -> obj.workplaceType)
      .add()

      .append(new KeyedCodec<>("Position", Vec3i.CODEC),
          (obj, value) -> obj.position = value,
          obj -> obj.position)
      .add()

      .append(new KeyedCodec<>("Rotation", Rotation.CODEC, false),
          (obj, value) -> obj.rotation = value,
          obj -> obj.rotation)
      .add()

      .append(new KeyedCodec<>("Level", Codec.INTEGER, false),
          (obj, value) -> obj.level = value,
          obj -> obj.level)
      .add()

      .append(
          new KeyedCodec<>("ConstructionMode", new EnumCodec<ConstructionMode>(ConstructionMode.class)),
          (obj, value) -> obj.constructionMode = value,
          (obj) -> obj.constructionMode)
      .add()

      .build();

  private UUID structureId;
  private StructureType workplaceType;
  private Vec3i position;
  private Rotation rotation = Rotation.None;
  private Integer level = 1;
  private ConstructionMode constructionMode;

  public SpawnWorkplaceTask() {
  }

  public SpawnWorkplaceTask(
      UUID structureId,
      StructureType workplaceType,
      Vec3i position,
      Rotation rotation,
      int level,
      ConstructionMode constructionMode) {
    this.structureId = structureId;
    this.workplaceType = workplaceType;
    this.position = position;
    this.rotation = rotation;
    this.level = level;
    this.constructionMode = constructionMode;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vec3i heartPos,
      VillageHeartComponent heart) {
    try {
      BeginNewConstruction spawnFunction = Utils.getBeginNewConstructionFunction(this.workplaceType);
      boolean spawned = spawnFunction.apply(
          this.structureId,
          chunk,
          this.position,
          this.rotation,
          heart,
          this.level,
          this.constructionMode);
      if (spawned) {
        this.callOnSuccess("BUILD_START_SUCCESS");
        return;
      }

      String message = "Could not start " + this.workplaceType
          + " construction for " + this.structureId + " at " + this.position;
      Logger.logWarning(message);
      this.callOnError("BUILD_START_FAILED", message);
    } catch (RuntimeException exception) {
      String message = "Failed to start " + this.workplaceType
          + " construction for " + this.structureId + " at " + this.position + ": "
          + exception.getMessage();
      Logger.logWarning(message);
      this.callOnError("BUILD_START_EXCEPTION", message);
      throw exception;
    }
  }
}
