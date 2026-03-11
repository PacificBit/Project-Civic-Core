package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.function.BiConsumer;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;

public class RegisterWorkplaceTask implements IVillageTask {
  public static final BuilderCodec<RegisterWorkplaceTask> CODEC = BuilderCodec
      .builder(RegisterWorkplaceTask.class, RegisterWorkplaceTask::new)

      .append(new KeyedCodec<>("Workplace", WorkplaceComponent.CODEC),
          (obj, value) -> obj.workplace = value,
          (obj) -> obj.workplace)
      .add()

      .build();

  private WorkplaceComponent workplace;

  public RegisterWorkplaceTask() {
  }

  public RegisterWorkplaceTask(WorkplaceComponent workplace) {
    this.workplace = workplace;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vector3i heartPos,
      VillageHeartComponent heart) {
    ProjectCivicCore.LOGGER
        .atInfo()
        .log("[RegisterWorkplaceTask] Registering workplace '" + this.workplace.getWorkplaceType() + "' with id: "
            + this.workplace.getUuid());
    heart.registerWorkplace(this.workplace);
  }

  @Override
  public void setOnError(BiConsumer<String, Object> callback) {
    throw new UnsupportedOperationException("Unimplemented method 'setOnError'");
  }

}
