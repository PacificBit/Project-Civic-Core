package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.farmer;

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

  private static final double MINIMUM_DISTANCE = 2.5;
  private static final String TILLABLE_SOIL = "Soil_Grass";

  public ReachNextTillableSoilSensor(BuilderSensorBase builderSensorBase) {
    super(builderSensorBase);
  }

  private final PositionProvider positionProvider = new PositionProvider();

  @Override
  public InfoProvider getSensorInfo() {
    return this.positionProvider;
  }

  @Override
  public boolean matches(Ref<EntityStore> ref, Role role, double dt, Store<EntityStore> store) {
    VillagerComponent villager = store.getComponent(ref, VillagerComponent.getComponentType());

    if (villager == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[ReachNextTillableSoilSensor] Could not find the villager component");
      return false;
    }

    Vector3i tillable = villager
        .getWorkerData()
        .getWorkareaSnapshot()
        .getBlockTypePosition()
        .get(TILLABLE_SOIL)
        .peek();
    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    double distanceFromBlock = transform.getPosition().distanceSquaredTo(tillable);

    if (distanceFromBlock < MINIMUM_DISTANCE) {
      this.positionProvider.clear();
      return false;
    }

    this.positionProvider.setTarget(tillable.toVector3d());

    return true;
  }

}
