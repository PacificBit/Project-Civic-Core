package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class ReachWorkplaceSensor extends VillagerSensorBase {

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
  public boolean matches(Ref<EntityStore> ref, Role role, double deltaTime, Store<EntityStore> store) {
    if (!super.matches(ref, role, deltaTime, store)) {
      return false;
    }

    WorkplaceComponent workplace = this.getWorkplaceComponent(ref, store);
    if (workplace == null) {
      return false;
    }

    Vector3i benchAccessPos = workplace.getBenchAccessPos();
    if (benchAccessPos == null) {
      this.positionProvider.clear();
      return false;
    }

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    Vector3d workplacePos = Utils.centerOf(benchAccessPos).toVector3d();
    double distanceFromWorkplace = transform.getPosition().distanceSquaredTo(workplacePos);

    if (distanceFromWorkplace < MINIMUM_DISTANCE) {
      this.positionProvider.clear();
      return false;
    }

    this.positionProvider.setTarget(workplacePos);

    return true;
  }

}
