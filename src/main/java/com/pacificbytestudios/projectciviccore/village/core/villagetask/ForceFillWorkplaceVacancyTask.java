package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.TownHallComponent;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.villagers.components.MayorComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;

@SerializableClass
public class ForceFillWorkplaceVacancyTask extends AVillageTask {
  public static final BuilderCodec<ForceFillWorkplaceVacancyTask> CODEC = ForceFillWorkplaceVacancyTaskCodec.CODEC;

  public ForceFillWorkplaceVacancyTask() {
  }

  public ForceFillWorkplaceVacancyTask(UUID workplace) {
    this.workplace = workplace;
  }

  UUID workplace;

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vec3i heartPos,
      VillageHeartComponent heart) {
    UUID candidate = findFirstUnemployed(heart);
    if (candidate == null) {
      candidate = findSecondaryWorkerAndUnregister(heart);
    }

    if (candidate == null) {
      Logger.logWarning("No villager available for workplace vacancy " + this.workplace);
      return;
    }

    heart.registerVillagerAsWorkerAt(candidate, this.workplace);
    heart.getWorkplaceFromId(this.workplace).registerAsBoss(candidate);
    UUID assignedCandidate = candidate;
    VillagerComponent assignedVillagerSnapshot = VillageVillagerEntitySync.snapshotVillager(heart, candidate);
    Logger.logInfo("Worker: " + candidate + " now is a worker");

    World world = chunk.getWorld();
    WorkplaceComponent workplaceComponent = heart.getWorkplaceFromId(this.workplace);
    if (workplaceComponent.getWorkplaceType().equals(StructureType.TOWN_HALL)) {
      Ref<EntityStore> villager = world.getEntityRef(assignedCandidate);
      Store<EntityStore> entityStore = world.getEntityStore().getStore();
      TownHallComponent townHall = resolveTownHallComponent(workplaceComponent);
      world.execute(() -> {
        VillageVillagerEntitySync.persistVillager(world, assignedCandidate, assignedVillagerSnapshot);

        MayorComponent mayorComponent = entityStore.getComponent(villager, MayorComponent.getComponentType());
        if (mayorComponent == null) {
          mayorComponent = new MayorComponent();
        }

        if (!mayorComponent.hasProject() && townHall != null && townHall.getVillageProjectPlan() != null) {
          mayorComponent.setExecutingVillagePlan(townHall.getVillageProjectPlan());
        }

        entityStore.putComponent(villager, MayorComponent.getComponentType(), mayorComponent);
      });
      Logger.logInfo("Worker: " + candidate + " now is a Mayor");
      return;
    }

    world.execute(() -> VillageVillagerEntitySync.persistVillager(world, assignedCandidate, assignedVillagerSnapshot));
  }

  private UUID findFirstUnemployed(VillageHeartComponent heart) {
    Set<UUID> unemployed = heart.getUnemployedRegistry();

    if (unemployed.size() == 0) {
      Logger.logInfo("No unemployed villagers found");
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

  private TownHallComponent resolveTownHallComponent(WorkplaceComponent workplaceComponent) {
    if (workplaceComponent == null || workplaceComponent.getWorkplaceRef() == null) {
      return null;
    }

    Ref<ChunkStore> workplaceRef = workplaceComponent.getWorkplaceRef().getRef();
    if (workplaceRef == null || !workplaceRef.isValid()) {
      return null;
    }

    return workplaceRef.getStore().getComponent(workplaceRef, TownHallComponent.getComponentType());
  }
}
