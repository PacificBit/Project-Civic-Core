package com.pacificbytestudios.projectciviccore.villagers.core.utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;

public abstract class VillagerSensorBase extends SensorBase {

  public VillagerSensorBase(BuilderSensorBase builderSensorBase) {
    super(builderSensorBase);
  }

  protected VillagerComponent getVillagerComponent(Ref<EntityStore> ref, Store<EntityStore> store) {
    VillagerComponent component = store.getComponent(ref, VillagerComponent.getComponentType());
    return require(component, "villager component");
  }

  protected VillagerWorkerComponent getWorkerDataComponent(Ref<EntityStore> ref, Store<EntityStore> store) {
    VillagerComponent villager = getVillagerComponent(ref, store);
    return villager != null ? require(villager.getWorkerData(), "worker data") : null;
  }

  protected WorkplaceComponent getWorkplaceComponent(Ref<EntityStore> ref, Store<EntityStore> store) {
    VillagerWorkerComponent workerData = getWorkerDataComponent(ref, store);
    return workerData != null ? require(workerData.getWorkplace(), "workplace component") : null;
  }

  protected <T> T require(T value, String what) {
    if (value == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[" + getClass().getSimpleName() + "] Could not find " + what);
    }
    return value;
  }
}
