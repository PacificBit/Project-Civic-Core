package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.utils.SpawnerFunctions.BeginUpgradeConstruction;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.enums.ConstructionMode;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudio.codecbutler.annotations.SerialName;

@SerializableClass
public class UpgradeBuildingTask extends AVillageTask {

  public static final BuilderCodec<UpgradeBuildingTask> CODEC = UpgradeBuildingTaskCodec.CODEC;

  StructureType structureType;
  Vec3i prefabAnchor;
  int targetLevel;
  @SerialName("ConstructionMode")
  ConstructionMode mode;

  public UpgradeBuildingTask() {
  }

  public UpgradeBuildingTask(
      StructureType structureType,
      Vec3i prefabAnchor,
      int targetLevel) {
    this(structureType, prefabAnchor, targetLevel, ConstructionMode.STAGED);
  }

  public UpgradeBuildingTask(
      StructureType structureType,
      Vec3i prefabAnchor,
      int targetLevel,
      ConstructionMode mode) {
    this.structureType = structureType;
    this.prefabAnchor = prefabAnchor;
    this.targetLevel = targetLevel;
    this.mode = mode;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vec3i heartPos,
      VillageHeartComponent heart) {
    BeginUpgradeConstruction function = Utils.getBeginUpgradeConstructionFunction(this.structureType);
    try {
      WorldChunk chunkToUse = Utils.getWorldChunk(chunk.getWorld(), this.prefabAnchor);
      boolean started = function.apply(chunkToUse, this.prefabAnchor, this.targetLevel, this.mode);
      if (started) {
        this.callOnSuccess("UPGRADE_START_SUCCESS");
        return;
      }

      String message = "Could not start " + this.structureType
          + " upgrade at " + this.prefabAnchor + " for target level " + this.targetLevel;
      Logger.logWarning(message);
      this.callOnError("UPGRADE_START_FAILED", message);
    } catch (RuntimeException exception) {
      String message = "Failed to start " + this.structureType
          + " upgrade at " + this.prefabAnchor + ": " + exception.getMessage();
      Logger.logWarning(message);
      this.callOnError("UPGRADE_START_EXCEPTION", message);
      throw exception;
    }
  }

}
