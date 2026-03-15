package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequestTracker;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class HasStorageRequestInPendingSensor extends VillagerSensorBase {

  public HasStorageRequestInPendingSensor(BuilderSensorBase builderSensorBase) {
    super(builderSensorBase);
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

    WorkplaceComponent workplace = this.getWorkplaceComponent(ref, store);
    VillageHeartComponent villageHeart = this.getVillageHeartComponent(ref, store);
    Ref<com.hypixel.hytale.server.core.universe.world.storage.ChunkStore> villageHeartRef = this.getVillageRef(ref,
        store);
    if (workplace == null) {
      return false;
    }

    return VillageStorageRequestTracker.hasPendingRequest(villageHeartRef, villageHeart, workplace.getUuid(), null);
  }

}
