package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.mayor;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.TownHallComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.MayorComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.villagers.core.work.mayor.MayorVillageAssessmentPlanner;
import com.pacificbytestudios.projectciviccore.villagers.core.work.mayor.MayorVillageAssessmentPlanner.AssessmentResult;

public class MakeVillageAssessment extends VillagerActionBase {

  private final MayorVillageAssessmentPlanner planner = new MayorVillageAssessmentPlanner();

  public MakeVillageAssessment(BuilderActionBase builderActionBase) {
    super(builderActionBase);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.canExecute(ref, role, sensorInfo, deltaTime, store)) {
      return false;
    }
    super.execute(ref, role, sensorInfo, deltaTime, store);

    MayorComponent mayorComponent = this.getMayorComponent(store, ref);

    TownHallComponent townHall = this.getComponentFromWorkplace(store, ref, TownHallComponent.getComponentType());
    VillageHeartComponent village = this.getVillageHeartComponent(ref, store);
    VillageTopologyComponent topology = this.getVillageTopologyComponent(ref, store);
    if (townHall == null || village == null || topology == null) {
      return false;
    }

    WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
    int today = worldTimeResource.getGameDateTime().getDayOfYear();

    AssessmentResult result = this.planner.assess(townHall, village, topology, today);
    if (!result.success()) {
      return false;
    }

    mayorComponent.setExecutingVillagePlan(result.projectPlan());

    return true;
  }

  private VillageTopologyComponent getVillageTopologyComponent(
      Ref<EntityStore> ref,
      Store<EntityStore> store) {
    Ref<ChunkStore> villageRef = this.getVillageRef(ref, store);
    if (!isRefValid(villageRef)) {
      return null;
    }

    return villageRef.getStore().getComponent(villageRef,
        VillageTopologyComponent.getComponentType());
  }

}
