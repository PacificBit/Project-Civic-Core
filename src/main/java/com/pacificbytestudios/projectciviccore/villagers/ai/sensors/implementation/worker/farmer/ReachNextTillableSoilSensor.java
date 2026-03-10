package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.farmer;

import java.util.ArrayDeque;
import java.util.Map;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;

public class ReachNextTillableSoilSensor extends SensorBase {

  private static final double MINIMUM_DISTANCE = 4.5;
  private static final String[] TILLABLE_SOIL = new String[] { "Soil_Grass", "Soil_Mud_Dry" };

  private final PositionProvider positionProvider = new PositionProvider();

  public ReachNextTillableSoilSensor(BuilderSensorBase builderSensorBase) {
    super(builderSensorBase);
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

    VillagerComponent villager = store.getComponent(ref, VillagerComponent.getComponentType());
    if (villager == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[ReachNextTillableSoilSensor] Could not find the villager component");
      this.positionProvider.clear();
      return false;
    }

    Map<String, ArrayDeque<Vector3i>> blockPositions = villager
        .getWorkerData()
        .getWorkareaSnapshot()
        .getBlockTypePosition();

    ArrayDeque<Vector3i> grassQueue = blockPositions.get(TILLABLE_SOIL[0]);
    ArrayDeque<Vector3i> mudQueue = blockPositions.get(TILLABLE_SOIL[1]);

    Vector3i tillable;
    if (grassQueue != null && !grassQueue.isEmpty()) {
      tillable = grassQueue.peekFirst();
    } else if (mudQueue != null && !mudQueue.isEmpty()) {
      tillable = mudQueue.peekFirst();
    } else {
      this.positionProvider.clear();
      return false;
    }

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[ReachNextTillableSoilSensor] Could not find transform component");
      this.positionProvider.clear();
      return false;
    }

    double distanceFromBlock = transform.getPosition().distanceSquaredTo(tillable);

    if (distanceFromBlock < MINIMUM_DISTANCE) {
      this.positionProvider.clear();
      return false;
    }

    this.positionProvider.setTarget(tillable.toVector3d());
    return true;
  }
}
