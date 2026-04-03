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
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.TownHallComponent;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.villagers.components.MayorComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;

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
    WorkplaceComponent workplaceComponent = heart.getWorkplaceFromId(this.workplace);
    if (workplaceComponent == null) {
      ProjectCivicCore.LOGGER.atWarning()
          .log("[TryJobAssignmentTask] Could not find workplace " + this.workplace + " to assign a worker");
      return;
    }

    Set<UUID> unemployed = heart.getUnemployedRegistry();

    if (unemployed.size() == 0) {
      ProjectCivicCore.LOGGER.atInfo().log("[TryJobAssignmentTask] No unemployed villagers found");
      return;
    }

    World world = chunk.getWorld();
    UUID candidate = workplaceComponent.getWorkplaceType().equals(StructureType.TOWN_HALL)
        ? findExistingMayorCandidate(unemployed, world)
        : null;
    if (candidate == null) {
      candidate = unemployed.iterator().next();
    }

    heart.registerVillagerAsWorkerAt(candidate, this.workplace);
    workplaceComponent.registerAsBoss(candidate);
    UUID assignedCandidate = candidate;
    VillagerComponent assignedVillagerSnapshot = VillageVillagerEntitySync.snapshotVillager(heart, candidate);
    ProjectCivicCore.LOGGER.atInfo().log("[TryJobAssignmentTask] Worker: " + candidate + " now is a worker");

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
      ProjectCivicCore.LOGGER.atInfo().log("[TryJobAssignmentTask] Worker: " + candidate + " now is a Mayor");
      return;
    }

    world.execute(() -> VillageVillagerEntitySync.persistVillager(world, assignedCandidate, assignedVillagerSnapshot));
  }

  private UUID findExistingMayorCandidate(Set<UUID> unemployed, World world) {
    if (unemployed == null || unemployed.isEmpty() || world == null) {
      return null;
    }

    Store<EntityStore> entityStore = world.getEntityStore().getStore();
    for (UUID candidate : unemployed) {
      Ref<EntityStore> villagerRef = world.getEntityRef(candidate);
      if (villagerRef == null || !villagerRef.isValid()) {
        continue;
      }

      MayorComponent mayorComponent = entityStore.getComponent(villagerRef, MayorComponent.getComponentType());
      if (mayorComponent != null) {
        return candidate;
      }
    }

    return null;
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
