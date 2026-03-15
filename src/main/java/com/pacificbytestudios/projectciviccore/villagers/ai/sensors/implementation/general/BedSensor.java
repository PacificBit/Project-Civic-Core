package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general;

import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;

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
    Vector3d vector3d = villager.getBedPosition().toVector3d();
    double distanceFromBed = transform.getPosition().distanceSquaredTo(vector3d);
    if (distanceFromBed == 0 || distanceFromBed < 0.5) {
      this.positionProvider.clear();
      return false;
    }

    this.positionProvider.setTarget(vector3d);

    return true;
  }

}
