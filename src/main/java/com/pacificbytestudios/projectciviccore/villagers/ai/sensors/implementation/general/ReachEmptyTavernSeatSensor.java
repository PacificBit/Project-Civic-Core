package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderReachEmptyTavernSeatSensor;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class ReachEmptyTavernSeatSensor extends VillagerSensorBase {
  private double stoppingRange;

  public ReachEmptyTavernSeatSensor(
      BuilderReachEmptyTavernSeatSensor builderSensorBase,
      BuilderSupport support) {
    super(builderSensorBase);
    this.stoppingRange = builderSensorBase.getStoppingRange(support);
    this.stoppingRange *= this.stoppingRange;
  }

  private final PositionProvider positionProvider = new PositionProvider();
  private Vec3d target = null;

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

    if (!this.isTriggered()) {
      this.triggered = true;

      VillagerComponent villagerComponent = this.getVillagerComponent(ref, store);
      if (villagerComponent == null || villagerComponent.getReservedInteractable() == null) {
        return false;
      }

      this.target = Vec3d.from(Utils.centerOf(villagerComponent.getReservedInteractable().to3i()));
      this.positionProvider.setTarget(this.target);
      return true;
    }

    TransformComponent transform = store.getComponent(ref,
        TransformComponent.getComponentType());

    if (transform.getPosition().distanceSquared(this.target) <= this.stoppingRange) {
      this.positionProvider.clear();
      return false;
    }
    return true;
  }

}
