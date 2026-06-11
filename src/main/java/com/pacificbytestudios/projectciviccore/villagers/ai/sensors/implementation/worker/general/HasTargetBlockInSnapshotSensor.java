package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general;

import java.util.ArrayDeque;
import java.util.Map;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
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
      ExecutionSupport executionSupport,
      double dt,
      Store<EntityStore> store) {
    if (!super.matches(ref, executionSupport, dt, store)) {
      return false;
    }

    WorkplaceAreaSnapshot snapshot = this.getAreaSnapshot(ref, store);
    if (snapshot == null) {
      return false;
    }

    Map<String, ArrayDeque<Vec3i>> blockPositions = snapshot.getBlockTypePosition();

    for (String targetBlockId : this.targetBlockIds) {
      ArrayDeque<Vec3i> queue = blockPositions.get(targetBlockId);
      if (queue != null && queue.size() > 0) {
        return true;
      }
    }

    return false;
  }

}
