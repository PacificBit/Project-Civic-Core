package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general;

import java.util.ArrayDeque;
import java.util.Map;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderHasTargetBlockInSnapshotSensor;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;

public class HasTargetBlockInSnapshotSensor extends SensorBase {
  private String[] targetBlockIds;

  public HasTargetBlockInSnapshotSensor(
      BuilderHasTargetBlockInSnapshotSensor builderSensorBase,
      BuilderSupport support) {
    super(builderSensorBase);
    this.targetBlockIds = builderSensorBase.getTargetBlockIds(support);
  }

  @Override
  public InfoProvider getSensorInfo() {
    return null;
  }

  @Override
  public boolean matches(
      Ref<EntityStore> ref,
      Role role,
      double dt,
      Store<EntityStore> store) {
    if (!super.matches(ref, role, dt, store)) {
      return false;
    }

    VillagerComponent villager = store.getComponent(ref, VillagerComponent.getComponentType());
    if (villager == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[HasTargetBlockInSnapshotSensor] Could not fetch the villager component");
      return false;
    }

    VillagerWorkerComponent workerData = villager.getWorkerData();
    if (workerData == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[HasTargetBlockInSnapshotSensor] Could not fetch workerData");
      return false;
    }

    Map<String, ArrayDeque<Vector3i>> blockPositions = workerData
        .getWorkareaSnapshot()
        .getBlockTypePosition();

    for (String targetBlockId : this.targetBlockIds) {
      ArrayDeque<Vector3i> queue = blockPositions.get(targetBlockId);
      if (queue != null && queue.size() > 0) {
        return true;
      }
    }

    return false;
  }

}
