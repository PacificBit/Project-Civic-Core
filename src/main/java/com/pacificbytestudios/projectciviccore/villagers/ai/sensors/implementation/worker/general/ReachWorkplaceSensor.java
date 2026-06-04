package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderReachWorkplaceSensor;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class ReachWorkplaceSensor extends VillagerSensorBase {

  private double stoppingRange;

  public ReachWorkplaceSensor(
      BuilderReachWorkplaceSensor builderSensorBase,
      BuilderSupport support) {
    super(builderSensorBase);
    this.stoppingRange = builderSensorBase.getStoppingRange(support);
    this.stoppingRange *= this.stoppingRange;
  }

  private final PositionProvider positionProvider = new PositionProvider();

  @Override
  public InfoProvider getSensorInfo() {
    return this.positionProvider;
  }

  @Override
  public boolean matches(Ref<EntityStore> ref, Role role, double deltaTime, Store<EntityStore> store) {
    if (!super.matches(ref, role, deltaTime, store)) {
      return false;
    }

    WorkplaceComponent workplace = this.getWorkplaceComponent(ref, store);
    if (workplace == null) {
      return false;
    }

    Vec3f benchAccessPos = workplace.getBenchAccessPos();
    if (benchAccessPos == null) {
      this.positionProvider.clear();
      return false;
    }

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    Vec3d workplacePos = Vec3d.from(benchAccessPos);
    double distanceFromWorkplace = transform.getPosition().distanceSquared(workplacePos);

    if (distanceFromWorkplace <= this.stoppingRange) {
      this.positionProvider.clear();
      return false;
    }

    this.positionProvider.setTarget(workplacePos);

    return true;
  }

}
