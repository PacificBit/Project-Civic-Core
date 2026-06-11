package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.innkeeper;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.village.components.workplace.tavern.TavernComponent;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.innkeeper.BuilderHasNextMealInPlannedSensor;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class HasNextMealInPlannedSensor extends VillagerSensorBase {
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
      ExecutionSupport executionSupport,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.matches(ref, executionSupport, deltaTime, store)) {
      return false;
    }

    Ref<ChunkStore> workplaceRef = this.getWorkplaceRef(ref, store);
    if (workplaceRef == null) {
      return false;
    }

    World world = store.getExternalData().getWorld();
    Store<ChunkStore> chunkStore = world.getChunkStore().getStore();

    TavernComponent tavernComponent = chunkStore.getComponent(workplaceRef, TavernComponent.getComponentType());
    if (tavernComponent == null) {
      Logger.logWarning("Could not fetch the tavern component");
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
