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
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;

public class UnregisterWorkplaceTask implements IVillageTask {
  public static final BuilderCodec<UnregisterWorkplaceTask> CODEC = BuilderCodec
      .builder(UnregisterWorkplaceTask.class, UnregisterWorkplaceTask::new)

      .append(new KeyedCodec<>("Workplace", Codec.UUID_BINARY),
          (obj, value) -> obj.workplace = value,
          (obj) -> obj.workplace)
      .add()

      .build();

  private UUID workplace;

  static {
    VillageTaskQueueComponent.registerTaskCodec(
        "UnregisterWorkplaceTask",
        UnregisterWorkplaceTask.class,
        UnregisterWorkplaceTask.CODEC);
  }

  public UnregisterWorkplaceTask() {
  }

  public UnregisterWorkplaceTask(UUID workplace) {
    this.workplace = workplace;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vector3i heartPos,
      VillageHeartComponent heart) {
    ProjectCivicCore.LOGGER
        .atInfo().log("[UnregisterWorkplaceTask] Unregistering workplace with id: " + this.workplace);
    heart.unregisterWorkplace(this.workplace);
  }

}
