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
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderReachNextTargetInSnapshotSensor;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class ReachNextTargetInSnapshotSensor extends VillagerSensorBase {
  private final PositionProvider positionProvider = new PositionProvider();
  private String[] blockIds;
  private double stoppingRange;
  private boolean useCenterOfBlock;

  public ReachNextTargetInSnapshotSensor(
      BuilderReachNextTargetInSnapshotSensor builderSensorBase,
      BuilderSupport support) {
    super(builderSensorBase);
    this.blockIds = builderSensorBase.getBlockIds(support);
    this.stoppingRange = builderSensorBase.getStoppingRange(support);
    this.stoppingRange = this.stoppingRange * this.stoppingRange;
    this.useCenterOfBlock = builderSensorBase.getUseCenterOfBlock(support);
  }

  @Override
  public InfoProvider getSensorInfo() {
    return this.positionProvider;
  }

  @Override
  public boolean matches(Ref<EntityStore> ref, Role role, double dt, Store<EntityStore> store) {
    if (!super.matches(ref, role, dt, store)) {
      this.positionProvider.clear();
      return false;
    }

    VillagerComponent villager = this.getVillagerComponent(ref, store);
    if (villager == null) {
      return false;
    }

    Map<String, ArrayDeque<Vector3i>> blockPositions = villager
        .getWorkerData()
        .getWorkareaSnapshot()
        .getBlockTypePosition();

    Vector3i target = null;
    for (String blockId : blockIds) {
      ArrayDeque<Vector3i> queue = blockPositions.get(blockId);
      if (queue != null && !queue.isEmpty()) {
        target = queue.peekFirst();
        break;
      }
    }

    if (target == null) {
      this.positionProvider.clear();
      return false;
    }

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[ReachNextTillableSoilSensor] Could not find transform component");
      this.positionProvider.clear();
      return false;
    }

    Vector3d targetPosition = this.useCenterOfBlock
        ? Utils.centerOf(target).toVector3d()
        : target.toVector3d();

    double distanceFromBlock = transform.getPosition().distanceSquaredTo(targetPosition);

    if (distanceFromBlock <= this.stoppingRange) {
      this.positionProvider.clear();
      return false;
    }

    this.positionProvider.setTarget(targetPosition);
    return true;
  }
}
