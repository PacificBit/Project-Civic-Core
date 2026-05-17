package com.pacificbytestudios.projectciviccore.villagers.core.utils;

import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.MayorComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;

public abstract class VillagerActionBase extends ActionBase {
  protected boolean suppressWarnings = false;

  public VillagerActionBase(BuilderActionBase builderActionBase) {
    super(builderActionBase);
  }

  protected VillagerComponent getVillagerComponent(Ref<EntityStore> ref, Store<EntityStore> store) {
    VillagerComponent component = store.getComponent(ref, VillagerComponent.getComponentType());
    return require(component, "villager component");
  }

  protected VillagerWorkerComponent getWorkerDataComponent(Ref<EntityStore> ref, Store<EntityStore> store) {
    VillagerComponent villager = getVillagerComponent(ref, store);
    return villager != null ? require(villager.getWorkerData(), "worker data") : null;
  }

  protected WorkplaceComponent getWorkplaceComponent(Ref<EntityStore> ref, Store<EntityStore> store) {
    VillagerWorkerComponent workerData = getWorkerDataComponent(ref, store);
    return workerData != null ? require(workerData.getWorkplace(), "workplace component") : null;
  }

  protected WorkplaceAreaSnapshot getAreaSnapshot(Ref<EntityStore> ref, Store<EntityStore> store) {
    VillagerWorkerComponent workerData = getWorkerDataComponent(ref, store);
    return workerData != null ? require(workerData.getWorkareaSnapshot(), "worker data snapshot") : null;
  }

  protected WorkplaceInteractablesMapComponent getWorkplaceInteractibleComponent(
      Ref<EntityStore> ref,
      Store<EntityStore> store) {
    Ref<ChunkStore> workplaceRef = getWorkplaceRef(ref, store);
    if (workplaceRef == null) {
      return null;
    }
    Store<ChunkStore> chunkStore = workplaceRef.getStore();
    return require(
        chunkStore.getComponent(
            workplaceRef,
            WorkplaceInteractablesMapComponent.getComponentType()),
        "workplace component");
  }

  protected UUID getVillagerId(
      Ref<EntityStore> ref,
      Store<EntityStore> store) {
    UUIDComponent component = store.getComponent(ref, UUIDComponent.getComponentType());
    return component != null ? require(component.getUuid(), "villager uuid") : null;
  }

  protected WorkplaceInteractablesMapComponent getVillageTavernInteractibleComponent(
      Ref<EntityStore> ref,
      Store<EntityStore> store) {
    VillageHeartComponent heart = getVillageHeartComponent(ref, store);
    if (heart == null) {
      return null;
    }

    Set<UUID> tavernWorkplaces = heart.getVillageTavernWorkplaces();
    if (tavernWorkplaces == null || tavernWorkplaces.isEmpty()) {
      return null;
    }

    UUID tavernId = tavernWorkplaces.iterator().next();
    WorkplaceComponent tavern = require(heart.getWorkplaceFromId(tavernId), "tavern workplace");
    if (tavern == null) {
      return null;
    }

    Ref<ChunkStore> tavernRef = require(tavern.getWorkplaceRef(), "workplace ref wrapper") != null
        ? require(tavern.getWorkplaceRef().getRef(), "workplace ref")
        : null;
    if (!isRefValid(tavernRef)) {
      return null;
    }

    return require(
        tavernRef.getStore().getComponent(
            tavernRef,
            WorkplaceInteractablesMapComponent.getComponentType()),
        "workplace interactable component");
  }

  protected Ref<ChunkStore> getReservedTavernRef(
      Ref<EntityStore> ref,
      Store<EntityStore> store) {
    VillagerComponent villager = getVillagerComponent(ref, store);
    if (villager == null) {
      return null;
    }

    Ref<ChunkStore> reservedTavernRef = villager.getReservedTavernRef();
    if (isRefValid(reservedTavernRef)) {
      return reservedTavernRef;
    }

    Vector3i reservedInteractable = villager.getReservedInteractable();
    if (reservedInteractable == null) {
      return null;
    }

    VillageHeartComponent heart = getVillageHeartComponent(ref, store);
    if (heart == null) {
      return null;
    }

    for (UUID tavernId : heart.getVillageTavernWorkplaces()) {
      WorkplaceComponent tavern = heart.getWorkplaceFromId(tavernId);
      if (tavern == null || tavern.getWorkplaceRef() == null) {
        continue;
      }

      Ref<ChunkStore> tavernRef = tavern.getWorkplaceRef().getRef();
      if (!isRefValid(tavernRef)) {
        continue;
      }

      WorkplaceInteractablesMapComponent interactables = tavernRef.getStore().getComponent(
          tavernRef,
          WorkplaceInteractablesMapComponent.getComponentType());
      if (interactables == null) {
        continue;
      }

      if (interactables.getInteractableAtPosition(
          com.pacificbytestudios.projectciviccore.village.enums.WorkplaceInteractibles.EATING_SPOT,
          reservedInteractable) == null) {
        continue;
      }

      villager.setReservedTavernLocation(tavern.getWorkplaceRef().clone());
      store.putComponent(ref, VillagerComponent.getComponentType(), villager);
      return tavernRef;
    }

    return null;
  }

  protected WorkplaceInteractablesMapComponent getReservedTavernInteractibleComponent(
      Ref<EntityStore> ref,
      Store<EntityStore> store) {
    Ref<ChunkStore> tavernRef = getReservedTavernRef(ref, store);
    if (!isRefValid(tavernRef)) {
      return null;
    }

    return require(
        tavernRef.getStore().getComponent(
            tavernRef,
            WorkplaceInteractablesMapComponent.getComponentType()),
        "reserved tavern interactable component");
  }

  protected NPCEntity getNPCEntity(Ref<EntityStore> ref, Store<EntityStore> store) {
    NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
    return require(npcEntity, "NPC entity");
  }

  protected ItemContainer getVillagerStorage(Ref<EntityStore> ref, Store<EntityStore> store) {
    NPCEntity npcEntity = getNPCEntity(ref, store);
    return npcEntity != null ? require(npcEntity.getInventory().getStorage(), "villager storage") : null;
  }

  protected Ref<ChunkStore> getVillageRef(Ref<EntityStore> ref, Store<EntityStore> store) {
    VillagerComponent villager = getVillagerComponent(ref, store);
    if (villager == null) {
      return null;
    }

    Ref<ChunkStore> villageRef = require(villager.getVillageHeartRef(), "village heart ref");
    return isRefValid(villageRef) ? villageRef : null;
  }

  protected Ref<ChunkStore> getWorkplaceRef(Ref<EntityStore> ref, Store<EntityStore> store) {
    WorkplaceComponent workplace = getWorkplaceComponent(ref, store);
    if (workplace == null) {
      return null;
    }

    if (require(workplace.getWorkplaceRef(), "workplace ref wrapper") == null) {
      return null;
    }

    Ref<ChunkStore> workplaceRef = require(workplace.getWorkplaceRef().getRef(), "workplace ref");
    return isRefValid(workplaceRef) ? workplaceRef : null;
  }

  protected VillageHeartComponent getVillageHeartComponent(Ref<EntityStore> ref, Store<EntityStore> store) {
    Ref<ChunkStore> villageRef = getVillageRef(ref, store);
    if (!isRefValid(villageRef)) {
      return null;
    }
    VillageHeartComponent villageHeart = villageRef.getStore().getComponent(
        villageRef,
        VillageHeartComponent.getComponentType());
    return require(villageHeart, "village heart component");
  }

  protected World getWorld(Store<EntityStore> store) {
    return store.getExternalData().getWorld();
  }

  protected Store<ChunkStore> getChunkStore(Store<EntityStore> store) {
    return getWorld(store).getChunkStore().getStore();
  }

  protected <T extends Component<ChunkStore>> T getComponent(
      Store<EntityStore> store,
      Ref<ChunkStore> ref,
      ComponentType<ChunkStore, T> componentType) {
    return require(getChunkStore(store).getComponent(ref, componentType), componentType.toString());
  }

  protected <T extends Component<ChunkStore>> T getComponentFromWorkplace(
      Store<EntityStore> store,
      Ref<EntityStore> ref,
      ComponentType<ChunkStore, T> componentType) {
    Ref<ChunkStore> workplace = getWorkplaceRef(ref, store);
    return workplace != null ? getComponent(store, workplace, componentType) : null;
  }

  protected MayorComponent getMayorComponent(
      Store<EntityStore> store,
      Ref<EntityStore> ref) {
    MayorComponent mayor = store.getComponent(ref, MayorComponent.getComponentType());
    if (mayor == null) {
      throw new IllegalStateException("Mayor does not have the mayor component");
    }
    return mayor;
  }

  protected boolean isRefValid(Ref<ChunkStore> ref) {
    return ref != null && ref.isValid();
  }

  protected Ref<ChunkStore> require(Ref<ChunkStore> value, String what) {
    if (!isRefValid(value) && !this.suppressWarnings) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[" + getClass().getSimpleName() + "] Could not find " + what);
    }
    return value;
  }

  protected <T> T require(T value, String what) {
    if (value == null && !this.suppressWarnings) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[" + getClass().getSimpleName() + "] Could not find " + what);
    }
    return value;
  }
}
