package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general;

import java.util.ArrayDeque;
import java.util.Map;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderReachWalkableSpaceNearSnapshotTargetSensor;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class ReachWalkableSpaceNearSnapshotTargetSensor extends VillagerSensorBase {
  private final PositionProvider positionProvider = new PositionProvider();
  private final String[] blockIds;
  private final double stoppingRangeSquared;
  private final boolean useCenterOfBlock;
  private Vector3i target = null;

  public ReachWalkableSpaceNearSnapshotTargetSensor(
      BuilderReachWalkableSpaceNearSnapshotTargetSensor builderSensorBase,
      BuilderSupport support) {
    super(builderSensorBase);
    this.blockIds = builderSensorBase.getBlockIds(support);

    double stoppingRange = builderSensorBase.getStoppingRange(support);
    this.stoppingRangeSquared = stoppingRange * stoppingRange;
    this.useCenterOfBlock = builderSensorBase.getUseCenterOfBlock(support);
  }

  @Override
  public InfoProvider getSensorInfo() {
    return this.positionProvider;
  }

  @Override
  public boolean matches(
      Ref<EntityStore> ref,
      Role role,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.matches(ref, role, deltaTime, store)) {
      this.positionProvider.clear();
      return false;
    }

    WorkplaceAreaSnapshot snapshot = this.getAreaSnapshot(ref, store);
    if (snapshot == null) {
      this.positionProvider.clear();
      return false;
    }

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      ProjectCivicCore.LOGGER.atSevere().log(
          "[ReachWalkableSpaceNearSnapshotTargetSensor] Could not find transform component");
      this.positionProvider.clear();
      return false;
    }

    this.findSnapshotTarget(snapshot.getBlockTypePosition());
    if (this.target == null) {
      this.positionProvider.clear();
      return false;
    }

    SnapshotTargetStandabilityInspector.Inspection standabilityInspection = SnapshotTargetStandabilityInspector.inspect(
        store.getExternalData().getWorld(),
        this.target,
        transform.getPosition(),
        this.useCenterOfBlock);
    Vector3i walkableTarget = standabilityInspection.getBestStandableCandidate();
    if (walkableTarget == null) {
      this.positionProvider.clear();
      return false;
    }

    Vector3d targetPosition = this.useCenterOfBlock
        ? Utils.centerOf(walkableTarget).toVector3d()
        : walkableTarget.toVector3d();

    double distanceFromTarget = transform.getPosition().distanceSquaredTo(targetPosition);
    if (distanceFromTarget <= this.stoppingRangeSquared) {
      this.positionProvider.clear();
      return false;
    }

    this.positionProvider.setTarget(targetPosition);
    return true;
  }

  private void findSnapshotTarget(Map<String, ArrayDeque<Vector3i>> blockPositions) {
    for (String blockId : this.blockIds) {
      ArrayDeque<Vector3i> queue = blockPositions.get(blockId);
      if (queue != null && !queue.isEmpty()) {
        Vector3i target = queue.peekFirst();
        if (target != null) {
          this.target = target;
          return;
        }
      }
    }
    return;
  }

}
