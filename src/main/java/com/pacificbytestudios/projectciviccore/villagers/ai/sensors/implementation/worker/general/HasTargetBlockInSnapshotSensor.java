package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general;

import java.util.ArrayDeque;
import java.util.Map;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderHasTargetBlockInSnapshotSensor;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class HasTargetBlockInSnapshotSensor extends VillagerSensorBase {
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

    WorkplaceAreaSnapshot snapshot = this.getAreaSnapshot(ref, store);
    if (snapshot == null) {
      return false;
    }

    Map<String, ArrayDeque<Vector3i>> blockPositions = snapshot.getBlockTypePosition();

    for (String targetBlockId : this.targetBlockIds) {
      ArrayDeque<Vector3i> queue = blockPositions.get(targetBlockId);
      if (queue != null && queue.size() > 0) {
        return true;
      }
    }

    return false;
  }

}
