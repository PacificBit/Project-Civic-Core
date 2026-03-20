package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.villagers.components.MayorComponent;

public class TryJobAssignmentTask extends AVillageTask {
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
    heart.registerVillagerAsWorkerAt(candidate, this.workplace);
    heart.getWorkplaceFromId(this.workplace).registerAsBoss(candidate);
    ProjectCivicCore.LOGGER.atInfo().log("[TryJobAssignmentTask] Worker: " + candidate + " now is a worker");

    if (heart.getWorkplaceFromId(this.workplace).getWorkplaceType().equals(StructureType.TOWN_HALL)) {
      World world = chunk.getWorld();
      Ref<EntityStore> villager = world.getEntityRef(candidate);
      Store<EntityStore> entityStore = world.getEntityStore().getStore();
      world.execute(() -> {
        entityStore.putComponent(villager, MayorComponent.getComponentType(), new MayorComponent());
      });
      ProjectCivicCore.LOGGER.atInfo().log("[TryJobAssignmentTask] Worker: " + candidate + " now is a Mayor");
    }

  }

}
