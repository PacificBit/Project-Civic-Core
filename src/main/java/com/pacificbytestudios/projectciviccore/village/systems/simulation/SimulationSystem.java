package com.pacificbytestudios.projectciviccore.village.systems.simulation;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageBuilderComponent;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.simulation.SimulationComponent;
import com.pacificbytestudios.projectciviccore.village.components.simulation.SimulationRunningComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.TownHallComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.MayorComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.work.mayor.MayorVillageAssessmentExecutor;
import com.pacificbytestudios.projectciviccore.villagers.core.work.mayor.MayorVillageAssessmentPlanner;
import com.pacificbytestudios.projectciviccore.villagers.core.work.mayor.MayorVillageAssessmentPlanner.AssessmentResult;

public class SimulationSystem extends EntityTickingSystem<ChunkStore> {
  private final MayorVillageAssessmentPlanner planner = new MayorVillageAssessmentPlanner();
  private final MayorVillageAssessmentExecutor executor = new MayorVillageAssessmentExecutor();

  @Override
  public void tick(
      float deltaTime,
      int index,
      ArchetypeChunk<ChunkStore> archetypeChunk,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    Ref<ChunkStore> villageRef = archetypeChunk.getReferenceTo(index);
    World world = store.getExternalData().getWorld();
    world.execute(() -> advanceOneDay(store, villageRef, world));
  }

  private void advanceOneDay(
      Store<ChunkStore> store,
      Ref<ChunkStore> villageRef,
      World world) {
    SimulationComponent simulation = store.getComponent(
        villageRef,
        SimulationComponent.getComponentType());

    if (simulation.getRemainingDays() <= 0) {
      stopSimulation(villageRef, store);
      return;
    }

    VillageHeartComponent village = store.getComponent(
        villageRef,
        VillageHeartComponent.getComponentType());
    VillageTopologyComponent topology = store.getComponent(
        villageRef,
        VillageTopologyComponent.getComponentType());

    WorkplaceComponent townHallWorkplace = village.getTownHall();
    if (townHallWorkplace == null || townHallWorkplace.getWorkplaceRef() == null) {
      ProjectCivicCore.LOGGER.atWarning()
          .log("[SimulationSystem] Could not resolve town hall while simulating days");
      stopSimulation(villageRef, store);
      return;
    }

    Ref<ChunkStore> townHallRef = townHallWorkplace.getWorkplaceRef().getRef();
    if (townHallRef == null || !townHallRef.isValid()) {
      ProjectCivicCore.LOGGER.atWarning()
          .log("[SimulationSystem] Town hall ref is invalid while simulating days");
      stopSimulation(villageRef, store);
      return;
    }

    TownHallComponent townHall = townHallRef.getStore().getComponent(
        townHallRef,
        TownHallComponent.getComponentType());
    if (townHall == null) {
      ProjectCivicCore.LOGGER.atWarning()
          .log("[SimulationSystem] Could not load town hall component while simulating days");
      stopSimulation(villageRef, store);
      return;
    }

    int today = resolveCurrentDay(world);
    AssessmentResult result = this.planner.assess(townHall, village, topology, today);
    if (!result.success() || result.projectPlan().getProjectList().isEmpty()) {
      ProjectCivicCore.LOGGER.atWarning()
          .log("[SimulationSystem] Mayor assessment failed while simulating days");
      stopSimulation(villageRef, store);
      return;
    }

    MayorComponent mayorComponent = Utils.getMayorComponent(world, village);
    mayorComponent.setExecutingVillagePlan(townHall.getVillageProjectPlan());
    this.executor.executeNext(world, village, townHall, mayorComponent, true);

    simulation.setRemainingDays(simulation.getRemainingDays() - 1);
    simulation.incrementProcessedDays();

    if (simulation.getRemainingDays() <= 0) {
      stopSimulation(villageRef, store);
      ProjectCivicCore.LOGGER.atInfo()
          .log("[SimulationSystem] Completed village simulation after "
              + simulation.getProcessedDays() + " simulated day(s)");
      return;
    }

    store.putComponent(villageRef, SimulationComponent.getComponentType(), simulation);
  }

  private int resolveCurrentDay(World world) {
    Store<EntityStore> entityStore = world.getEntityStore().getStore();
    WorldTimeResource worldTimeResource = entityStore.getResource(WorldTimeResource.getResourceType());
    if (worldTimeResource == null || worldTimeResource.getGameDateTime() == null) {
      return -1;
    }
    return worldTimeResource.getGameDateTime().getDayOfYear();
  }

  private void stopSimulation(
      Ref<ChunkStore> villageRef,
      Store<ChunkStore> store) {
    store.removeComponentIfExists(villageRef, SimulationRunningComponent.getComponentType());
    store.removeComponentIfExists(villageRef, SimulationComponent.getComponentType());
  }

  @Override
  public Query<ChunkStore> getQuery() {
    return Query.and(
        VillageHeartComponent.getComponentType(),
        VillageTopologyComponent.getComponentType(),
        SimulationComponent.getComponentType(),
        SimulationRunningComponent.getComponentType(),
        Query.not(VillageBuilderComponent.getComponentType()),
        Query.not(VillageTaskQueueComponent.getComponentType()));
  }
}
