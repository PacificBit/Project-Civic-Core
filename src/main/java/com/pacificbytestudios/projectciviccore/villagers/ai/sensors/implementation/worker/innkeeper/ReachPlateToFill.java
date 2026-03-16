package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.innkeeper;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.codecs.Interactable;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceInteractibles;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.innkeeper.BuilderReachPlateToFill;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class ReachPlateToFill extends VillagerSensorBase {
  private double stoppingRange;
  private final PositionProvider positionProvider = new PositionProvider();
  private Vector3d target = null;

  public ReachPlateToFill(BuilderReachPlateToFill builderSensorBase, BuilderSupport support) {
    super(builderSensorBase);
    this.stoppingRange = builderSensorBase.getStoppingRange(support);
    this.stoppingRange *= stoppingRange;
  }

  @Override
  public InfoProvider getSensorInfo() {
    return positionProvider;
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
      UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
      Interactable interactable = interactables.getInteractableOccupiedBy(
          WorkplaceInteractibles.EATING_SPOT,
          uuidComponent.getUuid());
      if (interactable == null) {
        this.positionProvider.clear();
        return false;
      }
      this.target = interactable.getInteractablePosition().toVector3d();
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
