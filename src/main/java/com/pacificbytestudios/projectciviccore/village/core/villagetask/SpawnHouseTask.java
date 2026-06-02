package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.UUID;

import org.joml.Vector3i;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.core.spawner.HouseSpawner;
import com.pacificbytestudios.projectciviccore.village.enums.ConstructionMode;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class SpawnHouseTask extends AVillageTask {

  public static final BuilderCodec<SpawnHouseTask> CODEC = BuilderCodec
      .builder(SpawnHouseTask.class, SpawnHouseTask::new)

      .append(new KeyedCodec<>("StructureId", Codec.UUID_BINARY, false),
          (obj, value) -> obj.structureId = value,
          obj -> obj.structureId)
      .add()

      .append(new KeyedCodec<>("Position", Vector3iUtil.CODEC),
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
  private Vector3i position;
  private Rotation rotation = Rotation.None;
  private Integer level = 1;
  private ConstructionMode constructionMode;

  public SpawnHouseTask() {
  }

  public SpawnHouseTask(
      UUID structureId,
      Vector3i position,
      Rotation rotation,
      int level,
      ConstructionMode constructionMode) {
    this.structureId = structureId;
    this.position = position;
    this.rotation = rotation;
    this.level = level;
    this.constructionMode = constructionMode;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vector3i heartPos,
      VillageHeartComponent heart) {
    try {
      boolean spawned = HouseSpawner.beginNewConstruction(
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

      String message = "Could not start house construction for " + this.structureId
          + " at " + this.position;
      Logger.logWarning(message);
      this.callOnError("BUILD_START_FAILED", message);
    } catch (RuntimeException exception) {
      String message = "Failed to start house construction for " + this.structureId
          + " at " + this.position + ": " + exception.getMessage();
      Logger.logWarning(message);
      this.callOnError("BUILD_START_EXCEPTION", message);
      throw exception;
    }
  }
}
