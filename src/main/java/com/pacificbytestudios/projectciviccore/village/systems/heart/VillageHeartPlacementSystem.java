package com.pacificbytestudios.projectciviccore.village.systems.heart;

import java.util.UUID;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.Utils.BlockContext;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageSpawnComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnVillagerTask;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;
import com.pacificbytestudios.projectciviccore.villagers.enums.VillagerRole;

public class VillageHeartPlacementSystem extends RefSystem<ChunkStore> {
  private static final VillagerRole[] SPAWNABLE_NPC = VillagerRole.values();
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  @Override
  public void onEntityAdded(
      Ref<ChunkStore> ref,
      AddReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    BlockContext context = Utils.getWorldBlockContext(ref, commandBuffer);

    assert context != null;

    WorldChunk chunk = context.chunk();
    World world = chunk.getWorld();
    Vector3i villageHeartPos = context.position();

    VillageSpawnComponent spawnComponent = store.getComponent(ref,
        VillageSpawnComponent.getComponentType());
    VillageHeartComponent villageHeart = store.getComponent(ref,
        VillageHeartComponent.getComponentType());
    int radius = spawnComponent.getSpawnNpcRange();

    villageHeart.setUuid(UUID.randomUUID());

    ClaimedAreaComponent area = Utils.claimVillageArea(villageHeart, villageHeartPos);
    villageStore.registerVillage(
        villageHeart.getUuid(),
        world.getName(),
        villageHeartPos,
        area);

    world.execute(() -> {
      VillageTaskQueueComponent taskComponent = new VillageTaskQueueComponent();
      for (int npcCount = 0; npcCount < spawnComponent.getInitialNpcCount(); npcCount++) {
        String npcRole = SPAWNABLE_NPC[npcCount % SPAWNABLE_NPC.length].getRole();
        taskComponent.registerTask(new SpawnVillagerTask(npcRole, radius));
      }

      Utils.safeVillageTaskEnqueue(ref, taskComponent);

      try {
        // INFO: villageHeartRef is invalid here
        store.removeComponent(ref,
            VillageSpawnComponent.getComponentType());
      } catch (Exception e) {
      }
    });
  }

  @Override
  public void onEntityRemove(
      Ref<ChunkStore> ref,
      RemoveReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
  }

  @Override
  public Query<ChunkStore> getQuery() {
    return Query.and(
        VillageHeartComponent.getComponentType(),
        VillageSpawnComponent.getComponentType());
  }
}
