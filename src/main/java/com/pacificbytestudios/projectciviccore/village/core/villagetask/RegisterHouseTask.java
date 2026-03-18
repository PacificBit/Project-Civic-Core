package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;

public class RegisterHouseTask extends AVillageTask {
  public static final BuilderCodec<RegisterHouseTask> CODEC = BuilderCodec
      .builder(RegisterHouseTask.class, RegisterHouseTask::new)

      .append(new KeyedCodec<>("House", HouseComponent.CODEC),
          (obj, value) -> obj.house = value,
          obj -> obj.house)
      .add()

      .build();

  private HouseComponent house;

  public RegisterHouseTask() {
  }

  public RegisterHouseTask(HouseComponent house) {
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
        .log("[RegisterHouseTask] Registering house with id: " + this.house.getUuid());
    heart.registerHouse(this.house);
  }
}
