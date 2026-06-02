package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.utils.SpawnerFunctions.AdvanceConstructionStage;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class ContinueBuildingTask extends AVillageTask {

  public static final BuilderCodec<ContinueBuildingTask> CODEC = BuilderCodec
      .builder(ContinueBuildingTask.class, ContinueBuildingTask::new)

      .append(new KeyedCodec<>("StructureType", new EnumCodec<StructureType>(StructureType.class)),
          (obj, value) -> obj.structureType = value,
          obj -> obj.structureType)
      .add()

      .append(new KeyedCodec<>("PrefabAnchor", Vec3i.CODEC),
          (obj, value) -> obj.prefabAnchor = value,
          obj -> obj.prefabAnchor)
      .add()

      .append(new KeyedCodec<>("Rotation", Rotation.CODEC, false),
          (obj, value) -> obj.rotation = value,
          obj -> obj.rotation)
      .add()

      .build();

  private StructureType structureType;
  private Vec3i prefabAnchor;
  private Rotation rotation;

  public ContinueBuildingTask() {
  }

  public ContinueBuildingTask(StructureType structureType, Vec3i prefabAnchor, Rotation rotation) {
    this.structureType = structureType;
    this.prefabAnchor = prefabAnchor;
    this.rotation = rotation;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vec3i heartPos,
      VillageHeartComponent heart) {
    AdvanceConstructionStage function = Utils.getAdvanceConstructionStageFunction(this.structureType);
    try {
      boolean advanced = function.apply(chunk, this.prefabAnchor, this.rotation);
      if (advanced) {
        this.callOnSuccess("CONTINUE_SUCCESS");
        return;
      }

      String message = "Could not advance " + this.structureType
          + " construction at " + this.prefabAnchor;
      Logger.logWarning(message);
      this.callOnError("CONTINUE_FAILED", message);
    } catch (RuntimeException exception) {
      String message = "Failed to advance " + this.structureType
          + " construction at " + this.prefabAnchor + ": " + exception.getMessage();
      Logger.logWarning(message);
      this.callOnError("CONTINUE_EXCEPTION", message);
      throw exception;
    }
  }

}
