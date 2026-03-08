package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.village.components.workplace.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;

public class ReachWorkplaceSensor extends SensorBase {

  private static final double MINIMUM_DISTANCE = 4.5;

  public ReachWorkplaceSensor(BuilderSensorBase builderSensorBase) {
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
    WorkplaceComponent workplace = villager.getWorkplace();

    if (workplace == null) {
      return false;
    }

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    Vector3d workplacePos = workplace.getBenchPos().toVector3d();
    double distanceFromWorkplace = transform.getPosition().distanceSquaredTo(workplacePos);

    if (distanceFromWorkplace < MINIMUM_DISTANCE) {
      this.positionProvider.clear();
      return false;
    }

    this.positionProvider.setTarget(workplacePos);

    return true;
  }

}
