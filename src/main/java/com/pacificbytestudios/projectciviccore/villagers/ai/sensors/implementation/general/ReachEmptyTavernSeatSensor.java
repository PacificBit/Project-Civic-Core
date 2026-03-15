package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.general;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceInteractibles;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderReachEmptyTavernSeatSensor;
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
  private Vector3d target = null;

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

    WorkplaceInteractablesMapComponent interactables = this.getWorkplaceInteractibleComponent(ref, store);
    if (interactables == null) {
      this.positionProvider.clear();
      return false;
    }

    if (!this.isTriggered()) {
      this.triggered = true;
      Vector3i position = interactables.getFirstInteractablePositionUnoccupied(WorkplaceInteractibles.CHAIR);
      if (position == null) {
        this.positionProvider.clear();
        return false;
      }
      UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
      interactables.registerAsOccupied(WorkplaceInteractibles.CHAIR, position, uuidComponent.getUuid());
      this.target = Utils.centerOf(position).toVector3d();
      this.positionProvider.setTarget(this.target);
      return true;
    }

    TransformComponent transform = store.getComponent(ref,
        TransformComponent.getComponentType());

    if (transform.getPosition().distanceSquaredTo(this.target) <= this.stoppingRange) {
      this.positionProvider.clear();
      return false;
    }
    return true;
  }

}
