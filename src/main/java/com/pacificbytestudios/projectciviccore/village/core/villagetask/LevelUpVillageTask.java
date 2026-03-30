package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;

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
    // TODO: here i will call the update street task for the VillageBuilderSystem
  }
}
