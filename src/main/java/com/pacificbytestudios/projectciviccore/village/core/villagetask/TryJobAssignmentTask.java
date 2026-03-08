package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.Set;
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

public class TryJobAssignmentTask implements IVillageTask {
  public static final BuilderCodec<TryJobAssignmentTask> CODEC = BuilderCodec
      .builder(TryJobAssignmentTask.class, TryJobAssignmentTask::new)

      .append(new KeyedCodec<>("Workplace", Codec.UUID_BINARY),
          (obj, value) -> obj.workplace = value,
          (obj) -> obj.workplace)
      .add()

      .build();

  public TryJobAssignmentTask() {
  }

  public TryJobAssignmentTask(UUID workplace) {
    this.workplace = workplace;
  }

  private UUID workplace;

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vector3i heartPos,
      VillageHeartComponent heart) {
    Set<UUID> unemployed = heart.getUnemployedRegistry();

    if (unemployed.size() == 0) {
      ProjectCivicCore.LOGGER.atInfo().log("[TryJobAssignmentTask] No unemployed villagers found");
      return;
    }

    UUID candidate = unemployed.iterator().next();

    heart.registerVillagerAsWorkerAt(candidate, workplace);

    ProjectCivicCore.LOGGER.atInfo().log("[TryJobAssignmentTask] Worker: " + candidate + " now is a worker");
  }

}
