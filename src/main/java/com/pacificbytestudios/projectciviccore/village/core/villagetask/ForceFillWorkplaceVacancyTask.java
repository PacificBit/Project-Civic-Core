package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.Map.Entry;
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
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineComponent;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.villagers.components.MayorComponent;

public class ForceFillWorkplaceVacancyTask extends AVillageTask {
  public static final BuilderCodec<ForceFillWorkplaceVacancyTask> CODEC = BuilderCodec
      .builder(ForceFillWorkplaceVacancyTask.class, ForceFillWorkplaceVacancyTask::new)

      .append(new KeyedCodec<>("Workplace", Codec.UUID_BINARY),
          (obj, value) -> obj.workplace = value,
          (obj) -> obj.workplace)
      .add()

      .build();

  public ForceFillWorkplaceVacancyTask() {
  }

  public ForceFillWorkplaceVacancyTask(UUID workplace) {
    this.workplace = workplace;
  }

  private UUID workplace;

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vector3i heartPos,
      VillageHeartComponent heart) {
    UUID candidate = findFirstUnemployed(heart);
    if (candidate == null) {
      candidate = findSecondaryWorkerAndUnregister(heart);
    }

    if (candidate == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[ForceFillWorkplaceVacancyTask]");
    }

    heart.registerVillagerAsWorkerAt(candidate, this.workplace);
    heart.getWorkplaceFromId(this.workplace).registerAsBoss(candidate);
    ProjectCivicCore.LOGGER.atInfo().log("[ForceFillWorkplaceVacancyTask] Worker: " + candidate + " now is a worker");

    if (heart.getWorkplaceFromId(this.workplace).getWorkplaceType().equals(StructureType.TOWN_HALL)) {
      World world = chunk.getWorld();
      Ref<EntityStore> villager = world.getEntityRef(candidate);
      Store<EntityStore> entityStore = world.getEntityStore().getStore();
      world.execute(() -> {
        entityStore.putComponent(villager, MayorComponent.getComponentType(), new MayorComponent());
      });
      ProjectCivicCore.LOGGER.atInfo().log("[ForceFillWorkplaceVacancyTask] Worker: " + candidate + " now is a Mayor");
    }

  }

  private UUID findFirstUnemployed(VillageHeartComponent heart) {
    Set<UUID> unemployed = heart.getUnemployedRegistry();

    if (unemployed.size() == 0) {
      ProjectCivicCore.LOGGER.atInfo().log("[ForceFillWorkplaceVacancyTask] No unemployed villagers found");
      return null;
    }

    return unemployed.iterator().next();
  }

  private UUID findSecondaryWorkerAndUnregister(VillageHeartComponent heart) {
    Set<UUID> storages = heart.getVillageStorageWorkplaces();
    for (Entry<UUID, WorkplaceComponent> entry : heart.getWorkplaceIdToWorkplace().entrySet()) {
      if (storages.contains(entry.getKey())) {
        continue;
      }

      WorkplaceComponent workplaceComponent = entry.getValue();
      if (workplaceComponent.getWorkers().size() == 0) {
        continue;
      }

      return unregisterWorkerFrom(heart, workplaceComponent);
    }
    return null;
  }

  private UUID unregisterWorkerFrom(VillageHeartComponent heart, WorkplaceComponent component) {
    UUID villagerId = component.getWorkers().iterator().next();
    component.unregisterWorker(villagerId);

    Ref<ChunkStore> chunkRef = component.getWorkplaceRef().getRef();

    if (component.getWorkplaceType() == StructureType.MINE) {
      MineComponent mineComponent = chunkRef.getStore().getComponent(
          chunkRef,
          MineComponent.getComponentType());
      mineComponent.unregisterFromGallery(villagerId);
    }

    heart.getVillager(villagerId).setWorkerData(null);
    return villagerId;
  }
}
