package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.mayor;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.TownHallComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.MayorComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.villagers.core.work.mayor.MayorVillageAssessmentExecutor;

public class ExecuteNextProjectStageAction extends VillagerActionBase {

  private final MayorVillageAssessmentExecutor executor = new MayorVillageAssessmentExecutor();

  public ExecuteNextProjectStageAction(BuilderActionBase builderActionBase) {
    super(builderActionBase);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      ExecutionSupport executionSupport,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    VillageHeartComponent village = this.getVillageHeartComponent(ref, store);
    TownHallComponent townHall = this.getComponentFromWorkplace(
        store,
        ref,
        TownHallComponent.getComponentType());
    MayorComponent mayorComponent = this.getMayorComponent(store, ref);

    if (village == null || townHall == null) {
      return false;
    }

    World world = store.getExternalData().getWorld();
    return this.executor.executeNext(world, village, townHall, mayorComponent);
  }

}
