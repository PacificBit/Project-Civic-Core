package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.utils.SpawnerFunctions.BeginUpgradeConstruction;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.enums.ConstructionMode;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;

public class UpgradeBuildingTask extends AVillageTask {

  public static final BuilderCodec<UpgradeBuildingTask> CODEC = BuilderCodec
      .builder(UpgradeBuildingTask.class, UpgradeBuildingTask::new)

      .append(new KeyedCodec<>("StructureType", new EnumCodec<StructureType>(StructureType.class)),
          (obj, value) -> obj.structureType = value,
          obj -> obj.structureType)
      .add()

      .append(new KeyedCodec<>("PrefabAnchor", Vector3i.CODEC),
          (obj, value) -> obj.prefabAnchor = value,
          obj -> obj.prefabAnchor)
      .add()

      .append(new KeyedCodec<>("TargetLevel", Codec.INTEGER),
          (obj, value) -> obj.targetLevel = value,
          obj -> obj.targetLevel)
      .add()

      .append(new KeyedCodec<>("ConstructionMode", new EnumCodec<ConstructionMode>(ConstructionMode.class)),
          (obj, value) -> obj.mode = value,
          obj -> obj.mode)
      .add()

      .build();

  private StructureType structureType;
  private Vector3i prefabAnchor;
  private int targetLevel;
  private ConstructionMode mode;

  public UpgradeBuildingTask() {
  }

  public UpgradeBuildingTask(
      StructureType structureType,
      Vector3i prefabAnchor,
      int targetLevel) {
    this(structureType, prefabAnchor, targetLevel, ConstructionMode.STAGED);
  }

  public UpgradeBuildingTask(
      StructureType structureType,
      Vector3i prefabAnchor,
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
      Vector3i heartPos,
      VillageHeartComponent heart) {
    BeginUpgradeConstruction function = Utils.getBeginUpgradeConstructionFunction(this.structureType);
    try {
      WorldChunk chunkToUse = Utils.getWorldChunk(chunk.getWorld(), this.prefabAnchor);
      boolean started = function.apply(chunkToUse, this.prefabAnchor, this.targetLevel, this.mode);
      if (started) {
        this.callOnSuccess("UPGRADE_START_SUCCESS");
        return;
      }

      String message = "[UpgradeBuildingTask] Could not start " + this.structureType
          + " upgrade at " + this.prefabAnchor + " for target level " + this.targetLevel;
      ProjectCivicCore.LOGGER.atWarning().log(message);
      this.callOnError("UPGRADE_START_FAILED", message);
    } catch (RuntimeException exception) {
      String message = "[UpgradeBuildingTask] Failed to start " + this.structureType
          + " upgrade at " + this.prefabAnchor + ": " + exception.getMessage();
      ProjectCivicCore.LOGGER.atWarning().log(message);
      this.callOnError("UPGRADE_START_EXCEPTION", message);
      throw exception;
    }
  }

}
