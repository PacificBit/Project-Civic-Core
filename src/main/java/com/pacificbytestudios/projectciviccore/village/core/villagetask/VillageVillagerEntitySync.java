package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;

final class VillageVillagerEntitySync {

  private VillageVillagerEntitySync() {
  }

  static VillagerComponent snapshotVillager(VillageHeartComponent heart, UUID villagerId) {
    if (heart == null || villagerId == null) {
      return null;
    }

    VillagerComponent villager = heart.getVillager(villagerId);
    return villager == null ? null : villager.clone();
  }

  static Map<UUID, VillagerComponent> snapshotVillagers(
      VillageHeartComponent heart,
      Collection<UUID> villagerIds) {
    Map<UUID, VillagerComponent> snapshots = new HashMap<>();
    if (heart == null || villagerIds == null) {
      return snapshots;
    }

    for (UUID villagerId : villagerIds) {
      VillagerComponent villagerSnapshot = snapshotVillager(heart, villagerId);
      if (villagerSnapshot != null) {
        snapshots.put(villagerId, villagerSnapshot);
      }
    }

    return snapshots;
  }

  static void persistVillager(World world, UUID villagerId, VillagerComponent villagerSnapshot) {
    if (world == null || villagerId == null || villagerSnapshot == null) {
      return;
    }

    Ref<EntityStore> villagerRef = world.getEntityRef(villagerId);
    if (villagerRef == null || !villagerRef.isValid()) {
      return;
    }

    Store<EntityStore> entityStore = world.getEntityStore().getStore();
    entityStore.putComponent(villagerRef, VillagerComponent.getComponentType(), villagerSnapshot);
  }

  static void persistVillagers(World world, Map<UUID, VillagerComponent> villagerSnapshots) {
    if (world == null || villagerSnapshots == null || villagerSnapshots.isEmpty()) {
      return;
    }

    for (Map.Entry<UUID, VillagerComponent> entry : villagerSnapshots.entrySet()) {
      persistVillager(world, entry.getKey(), entry.getValue());
    }
  }
}
