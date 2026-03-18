package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;

public class UnregisterHouseTask extends AVillageTask {
  public static final BuilderCodec<UnregisterHouseTask> CODEC = BuilderCodec
      .builder(UnregisterHouseTask.class, UnregisterHouseTask::new)

      .append(new KeyedCodec<>("House", Codec.UUID_BINARY),
          (obj, value) -> obj.house = value,
          obj -> obj.house)
      .add()

      .build();

  private UUID house;

  public UnregisterHouseTask() {
  }

  public UnregisterHouseTask(UUID house) {
    this.house = house;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vector3i heartPos,
      VillageHeartComponent heart) {
    ProjectCivicCore.LOGGER
        .atInfo()
        .log("[UnregisterHouseTask] Unregistering house with id: " + this.house);
    heart.unregisterHouse(this.house);
  }
}
