package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.utils.SpawnerFunctions.AdvanceConstructionStage;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;

@SerializableClass
public class ContinueBuildingTask extends AVillageTask {

  public static final BuilderCodec<ContinueBuildingTask> CODEC = ContinueBuildingTaskCodec.CODEC;

  StructureType structureType;
  Vec3i prefabAnchor;
  Rotation rotation;

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
      boolean advanced = function.apply(chunk, this.prefabAnchor, this.rotation, heart);
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
