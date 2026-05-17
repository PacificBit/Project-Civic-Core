package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.enums.ConstructionMode;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;

public class LevelUpVillageTask extends AVillageTask {
  public static final BuilderCodec<LevelUpVillageTask> CODEC = BuilderCodec
      .builder(LevelUpVillageTask.class, LevelUpVillageTask::new)

      .append(new KeyedCodec<>("Level", Codec.SHORT),
          (obj, value) -> obj.level = value,
          (obj) -> obj.level)
      .add()

      .build();

  private Short level;

  public LevelUpVillageTask() {
  }

  public LevelUpVillageTask(Short level) {
    this.level = level;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vector3i heartPos,
      VillageHeartComponent heart) {
    if (this.level > heart.getConfig().getMaxLevel() || this.level < 0) {
      ProjectCivicCore.LOGGER.atSevere().log("[LevelUpVillageTask] Received a village level too high: " + this.level);
      return;
    }
    heart.setLevel(this.level);
    enqueueTownHallUpgradeIfNeeded(heartRef, heart);
    // TODO: here i will call the update street task for the VillageBuilderSystem
  }

  private void enqueueTownHallUpgradeIfNeeded(
      Ref<ChunkStore> heartRef,
      VillageHeartComponent heart) {
    WorkplaceComponent townHall;
    try {
      townHall = heart.getTownHall();
    } catch (IllegalArgumentException exception) {
      ProjectCivicCore.LOGGER.atWarning()
          .log("[LevelUpVillageTask] Could not resolve town hall for village level-up: "
              + exception.getMessage());
      return;
    }

    if (townHall == null) {
      ProjectCivicCore.LOGGER.atWarning()
          .log("[LevelUpVillageTask] Village leveled up to " + this.level
              + " without a registered town hall to upgrade");
      return;
    }

    if (townHall.getLevel() != null && townHall.getLevel() >= this.level) {
      return;
    }

    if (townHall.getBenchPos() == null) {
      ProjectCivicCore.LOGGER.atWarning()
          .log("[LevelUpVillageTask] Could not upgrade town hall to level " + this.level
              + " because the bench position is missing");
      return;
    }

    VillageTaskQueueComponent taskQueue = new VillageTaskQueueComponent();
    taskQueue.registerTask(new UpgradeBuildingTask(
        StructureType.TOWN_HALL,
        townHall.getBenchPos().clone(),
        this.level,
        ConstructionMode.COMPLETED));
    Utils.safeVillageTaskEnqueue(heartRef, taskQueue);
  }
}
