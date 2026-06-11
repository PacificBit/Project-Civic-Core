package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general;

import java.util.UUID;

import com.hypixel.hytale.builtin.mounts.BlockMountAPI;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.Interactable;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.InteractablesType;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderMountMountableAction;
import com.pacificbytestudios.projectciviccore.villagers.core.events.SitOnChairEvent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;

public class MountMountableAction extends VillagerActionBase {
  private InteractablesType interactibleGroup;
  private StructureType structureType;

  public MountMountableAction(
      BuilderMountMountableAction builderActionBase,
      BuilderSupport support) {
    super(builderActionBase);

    this.interactibleGroup = builderActionBase.getInteractibleType(support);
    this.structureType = builderActionBase.getStructureType(support);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      ExecutionSupport executionSupport,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.canExecute(ref, executionSupport, sensorInfo, deltaTime, store)) {
      return false;
    }
    super.execute(ref, executionSupport, sensorInfo, deltaTime, store);

    return switch (this.structureType) {
      case TAVERN -> handleTavern(ref, store);
      case HOUSE -> throw new IllegalArgumentException("Structure type: " + this.structureType + " not supported");
      case FARM -> throw new IllegalArgumentException("Structure type: " + this.structureType + " not supported");
      case SAWMILL -> throw new IllegalArgumentException("Structure type: " + this.structureType + " not supported");
      case MINE -> throw new IllegalArgumentException("Structure type: " + this.structureType + " not supported");
      case STORAGE -> throw new IllegalArgumentException("Structure type: " + this.structureType + " not supported");
      default -> throw new IllegalArgumentException("Structure type: " + this.structureType + " not supported");
    };
  }

  private boolean handleTavern(
      Ref<EntityStore> ref,
      Store<EntityStore> store) {
    WorkplaceInteractablesMapComponent interactables = this.getReservedTavernInteractibleComponent(ref, store);
    if (interactables == null) {
      return false;
    }

    UUID villagerUUID = store.getComponent(ref, UUIDComponent.getComponentType()).getUuid();
    Interactable interactable = interactables.getInteractableOccupiedBy(this.interactibleGroup, villagerUUID);
    if (interactable == null) {
      return false;
    }

    return switch (this.interactibleGroup) {
      case EATING_SPOT -> handleChairInteratible(interactable.getInteractablePosition().to3i(), ref, store);
      default -> throw new IllegalArgumentException("Interactible type: " + this.interactibleGroup + " not supported");
    };
  }

  private boolean handleChairInteratible(
      Vec3i target,
      Ref<EntityStore> ref,
      Store<EntityStore> store) {

    SitOnChairEvent event = new SitOnChairEvent(target);
    store.invoke(ref, event);
    BlockMountAPI.BlockMountResult result = event.getResult();

    if (result instanceof BlockMountAPI.Mounted) {
      return true;
    }

    return false;
  }
}
