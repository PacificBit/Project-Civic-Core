package com.pacificbytestudios.projectciviccore.village.systems.heart;

import java.util.UUID;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.Utils.BlockContext;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageSpawnComponent;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class VillageHeartLoadUnloadSystem extends RefSystem<ChunkStore> {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  @Override
  public void onEntityAdded(
      Ref<ChunkStore> ref,
      AddReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    if (reason.equals(AddReason.SPAWN)) {
      Logger.logInfo("Skippin: " + reason);
      return;
    }
    Logger.logInfo("Loading reason: " +
        reason);

    BlockContext context = Utils.getWorldBlockContext(ref, commandBuffer);

    if (context == null) {
      Logger.logInfo("No context found");
      return;
    }

    WorldChunk chunk = context.chunk();

    EntityStore entityStore = context.chunk().getWorld().getEntityStore();
    Store<EntityStore> storeEntityStore = entityStore.getStore();
    chunk.getWorld().execute(() -> {
      VillageHeartComponent heart = store.getComponent(ref,
          VillageHeartComponent.getComponentType());

      if (heart == null) {
        Logger.logWarning("Could not find the heart component");
        return;
      }

      for (UUID uuid : heart.getVillagersRegistry()) {
        Ref<EntityStore> npcRef = entityStore.getRefFromUUID(uuid);
        if (npcRef == null || !npcRef.isValid()) {
          Logger.logWarning("Reference to npc is invalid");
          continue;
        }

        VillagerComponent npcComponent = storeEntityStore.getComponent(npcRef,
            VillagerComponent.getComponentType());
        if (npcComponent == null) {
          Logger.logWarning("Villager component not found");
          continue;
        }
        heart.registerVillager(uuid, npcComponent);
      }

      if (heart.getClaimedArea() == null) {
        Utils.claimVillageArea(heart, context.position());
      }

      villageStore.registerVillage(
          heart.getUuid(),
          chunk.getWorld().getName(),
          context.position(),
          heart.getClaimedArea());
    });
  }

  @Override
  public void onEntityRemove(
      Ref<ChunkStore> ref,
      RemoveReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    Logger.logInfo("Unloading reason: " + reason);

    VillageHeartComponent heart = store.getComponent(ref,
        VillageHeartComponent.getComponentType());

    if (heart == null) {
      Logger.logInfo("Could not find the heart component");
      return;
    }

    villageStore.unregisterVillage(heart.getUuid());
  }

  @Override
  public Query<ChunkStore> getQuery() {
    return Query.and(
        VillageHeartComponent.getComponentType(),
        Query.not(VillageSpawnComponent.getComponentType()));
  }
}
