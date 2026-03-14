package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.innkeeper;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.tavern.TavernComponent;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.innkeeper.BuilderHasNextMealInPlannedSensor;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;

public class HasNextMealInPlannedSensor extends SensorBase {
  private boolean assignMealToSelfIfPossible;

  public HasNextMealInPlannedSensor(
      BuilderHasNextMealInPlannedSensor builderSensorBase,
      BuilderSupport support) {
    super(builderSensorBase);
    this.assignMealToSelfIfPossible = builderSensorBase.getAssignMealToSelfFlag(support);
  }

  @Override
  public InfoProvider getSensorInfo() {
    return null;
  }

  @Override
  public boolean matches(
      Ref<EntityStore> ref,
      Role role,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.matches(ref, role, deltaTime, store)) {
      return false;
    }

    VillagerComponent villagerComponent = store.getComponent(ref, VillagerComponent.getComponentType());
    if (villagerComponent == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[HasNextMealInPlannedSensor] Could not fetch villager component");
      return false;
    }

    VillagerWorkerComponent workerComponent = villagerComponent.getWorkerData();
    if (workerComponent == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[HasNextMealInPlannedSensor] Could not fetch worker data for worker.");
      return false;
    }

    WorkplaceComponent workplaceComponent = workerComponent.getWorkplace();
    if (workplaceComponent == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[HasNextMealInPlannedSensor] Could not fetch workplace for worker.");
      return false;
    }

    World world = store.getExternalData().getWorld();
    Store<ChunkStore> chunkStore = world.getChunkStore().getStore();

    Ref<ChunkStore> workplaceRef = workplaceComponent.getWorkplaceRef().getRef();
    if (workplaceRef == null || !workplaceRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[HasNextMealInPlannedSensor] Invalid workplace ref");
      return false;
    }

    TavernComponent tavernComponent = chunkStore.getComponent(workplaceRef, TavernComponent.getComponentType());
    if (tavernComponent == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[HasNextMealInPlannedSensor] Could not fetch the tavern component");
      return false;
    }

    UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
    UUID workerUuid = uuidComponent.getUuid();

    boolean isThereAMealInQueue = tavernComponent.hasMealInQueue();
    boolean hasAssignedMeal = tavernComponent.getAssignedRecipe(workerUuid) != null;

    if (isThereAMealInQueue && !hasAssignedMeal && this.assignMealToSelfIfPossible) {
      String meal = tavernComponent.getNextMealToPrepare();
      tavernComponent.removeMealFromPlan(meal);
      tavernComponent.registerWorkerToRecipe(workerUuid, meal);
      hasAssignedMeal = tavernComponent.getAssignedRecipe(workerUuid) != null;
    }

    return isThereAMealInQueue || hasAssignedMeal;
  }

}
