package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;

public class UnmountMountableAction extends VillagerActionBase {

  public UnmountMountableAction(BuilderActionBase builderActionBase) {
    super(builderActionBase);
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

    Ref<ChunkStore> reservedTavernRef = this.getReservedTavernRef(ref, store);
    VillagerComponent villagerComponent = this.getVillagerComponent(ref, store);
    UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
    WorkplaceInteractablesMapComponent tavernInteractables = this.getReservedTavernInteractibleComponent(ref, store);
    if (reservedTavernRef == null && tavernInteractables != null) {
      reservedTavernRef = this.getReservedTavernRef(ref, store);
    }
    if (uuidComponent != null && tavernInteractables != null) {
      boolean released = tavernInteractables.releaseInteractablesOccupiedBy(uuidComponent.getUuid());
      if (released && reservedTavernRef != null) {
        reservedTavernRef.getStore().putComponent(
            reservedTavernRef,
            WorkplaceInteractablesMapComponent.getComponentType(),
            tavernInteractables);
        World world = store.getExternalData().getWorld();
        if (villagerComponent != null &&
            villagerComponent.getReservedTavernLocation() != null &&
            villagerComponent.getReservedTavernLocation().getPosition() != null) {
          Utils.markNeedSaving(world, villagerComponent.getReservedTavernLocation().getPosition());
        }
      }
    }

    if (villagerComponent != null &&
        (villagerComponent.getReservedInteractable() != null ||
            villagerComponent.getReservedTavernLocation() != null)) {
      villagerComponent.clearReservedTavernReservation();
      store.putComponent(ref, VillagerComponent.getComponentType(), villagerComponent);
    }

    store.tryRemoveComponent(ref, MountedComponent.getComponentType());
    return true;
  }

}
