package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class BedSensor extends VillagerSensorBase {

  public BedSensor(BuilderSensorBase builderSensorBase) {
    super(builderSensorBase);
  }

  private final PositionProvider positionProvider = new PositionProvider();

  @Override
  public InfoProvider getSensorInfo() {
    return this.positionProvider;
  }

  @Override
  public boolean matches(Ref<EntityStore> ref, Role role, double dt, Store<EntityStore> store) {
    VillagerComponent villager = this.getVillagerComponent(ref, store);
    if (villager == null) {
      return false;
    }

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    Vec3i bedPositionV3i = villager.getBedPosition();
    if (bedPositionV3i == null) {
      return false;
    }
    Vec3d bedPosition = Vec3d.from(Utils.centerOf(bedPositionV3i));
    double distanceFromBed = transform.getPosition().distanceSquared(bedPosition);
    if (distanceFromBed == 0 || distanceFromBed < 0.5) {
      this.positionProvider.clear();
      return false;
    }

    this.positionProvider.setTarget(bedPosition);

    return true;
  }

}
