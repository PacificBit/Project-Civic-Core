package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class IsBossWorker extends VillagerSensorBase {

  public IsBossWorker(BuilderSensorBase builderSensorBase) {
    super(builderSensorBase);
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

    VillagerWorkerComponent workerData = this.getWorkerDataComponent(ref, store);
    if (workerData == null) {
      return false;
    }

    UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
    UUID bossId = workerData.getWorkplace().getBossId();
    return bossId != null && bossId.equals(uuidComponent.getUuid());
  }

}
