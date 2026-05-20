package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general;

import java.util.ArrayDeque;
import java.util.Map;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderReachNextTargetInSnapshotSensor;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class ReachNextTargetInSnapshotSensor extends VillagerSensorBase {
  // private static final double NUDGE_DISTANCE = 0.75;

  private final PositionProvider positionProvider = new PositionProvider();
  private Vector3d target = null;
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
  public boolean matches(Ref<EntityStore> ref, Role role, double deltaTime, Store<EntityStore> store) {
    if (!super.matches(ref, role, deltaTime, store)) {
      this.positionProvider.clear();
      return false;
    }

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[ReachNextTillableSoilSensor] Could not find transform component");
      this.positionProvider.clear();
      return false;
    }

    if (!this.isTriggered()) {
      this.triggered = true;

      VillagerComponent villager = this.getVillagerComponent(ref, store);
      if (villager == null) {
        this.positionProvider.clear();
        this.triggered = false;
        return false;
      }

      Map<String, ArrayDeque<Vector3i>> blockPositions = villager
          .getWorkerData()
          .getWorkareaSnapshot()
          .getBlockTypePosition();

      Vector3i nextTarget = null;
      for (String blockId : this.blockIds) {
        ArrayDeque<Vector3i> queue = blockPositions.get(blockId);
        if (queue != null && !queue.isEmpty()) {
          nextTarget = queue.peekFirst();
          break;
        }
      }

      if (nextTarget == null) {
        this.positionProvider.clear();
        this.triggered = false;
        return false;
      }

      this.target = this.useCenterOfBlock
          ? VectorUtil.toVector3d(Utils.centerOf(nextTarget))
          : VectorUtil.toVector3d(nextTarget);

      this.positionProvider.setTarget(this.target);
      return true;
    }

    if (this.target == null) {
      this.positionProvider.clear();
      this.triggered = false;
      return false;
    }

    double distanceFromBlock = transform.getPosition().distanceSquared(this.target);

    if (distanceFromBlock <= this.stoppingRange) {
      this.positionProvider.clear();
      this.target = null;
      this.triggered = false;
      return false;
    }

    // TODO: find a solution for when they get stuck
    // Vector3d movementTarget = this.target;
    // if (role.getActiveMotionController().isObstructed()) {
    // Vector3d direction = new Vector3d(this.target).sub(transform.getPosition());
    // direction.y = 0.0;
    //
    // if (!direction.closeToZero(0.001)) {
    // Vector3d sideways = new Vector3d(direction.x, 0.0, -direction.z)
    // .normalize()
    // .mul(NUDGE_DISTANCE);
    // movementTarget = new Vector3d(transform.getPosition()).add(sideways);
    // }
    // }

    this.positionProvider.setTarget(this.target);
    return true;
  }
}
