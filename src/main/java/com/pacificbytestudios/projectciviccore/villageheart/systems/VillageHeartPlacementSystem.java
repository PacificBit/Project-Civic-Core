package com.pacificbytestudios.projectciviccore.villageheart.systems;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.FastRandom;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.villageheart.components.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.villageheart.components.VillageSpawnComponent;
import com.pacificbytestudios.projectciviccore.villageheart.registry.VillageHeartRegistry;
import com.pacificbytestudios.projectciviccore.villageheart.registry.VillageHeartRegistry.ChunkLocation;
import com.pacificbytestudios.projectciviccore.villagers.codecs.VillageHeartLocation;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;

import it.unimi.dsi.fastutil.Pair;

public class VillageHeartPlacementSystem extends RefSystem<ChunkStore> {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  private static final String[] SPAWNABLE_NPC = new String[] {
      "Female_Unemployed_Civilian",
      "Male_Unemployed_Civilian"
  };

  private FastRandom random = new FastRandom();

  @Override
  public void onEntityAdded(
      Ref<ChunkStore> villageHeartRef,
      AddReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    World world = store.getExternalData().getWorld();
    Vector3i blockPos = Utils.getWorldBlockPosition(villageHeartRef, commandBuffer);

    assert blockPos != null;

    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));
    villageStore.registerVillage(new ChunkLocation(chunk.getX(), chunk.getZ()), villageHeartRef);

    VillageSpawnComponent spawnComponent = store.getComponent(villageHeartRef,
        VillageSpawnComponent.getComponentType());
    VillageHeartComponent villageCoreComponent = store.getComponent(villageHeartRef,
        VillageHeartComponent.getComponentType());
    int radius = spawnComponent.getSpawnNpcRange();

    VillageHeartLocation villageHeartLocation = new VillageHeartLocation();
    villageHeartLocation.setPosition(blockPos);
    villageHeartLocation.setWorldName(world.getName());

    world.execute(() -> {
      Store<EntityStore> entityStore = world.getEntityStore().getStore();

      Vector3f rotation = new Vector3f(0f, 0f, 0f);

      for (int npcCount = 0; npcCount < spawnComponent.getInitialNpcCount(); npcCount++) {
        Vector3d spawnPos = newSpawnPos(blockPos, radius);
        Ref<EntityStore> npcRef = spawnNpc(entityStore, spawnPos, rotation, npcCount);

        assert npcRef != null;

        UUIDComponent uuidComponent = entityStore.getComponent(npcRef, UUIDComponent.getComponentType());
        villageCoreComponent.registerNpc(uuidComponent.getUuid());

        VillagerComponent villagerComponent = new VillagerComponent();
        villagerComponent.setVillageHeartLocation(villageHeartLocation.clone());
        entityStore.putComponent(npcRef, VillagerComponent.getComponentType(), villagerComponent);
      }

      store.removeComponent(villageHeartRef,
          VillageSpawnComponent.getComponentType());
    });
  }

  private Vector3d newSpawnPos(Vector3i blockPos, int radius) {
    double angle = random.nextDouble() * Math.PI * 2.0;

    double distance = Math.sqrt(random.nextDouble()) * radius;

    double offsetX = Math.cos(angle) * distance;
    double offsetZ = Math.sin(angle) * distance;

    Vector3d spawnPos = new Vector3d(
        blockPos.getX() + offsetX,
        blockPos.getY(),
        blockPos.getZ() + offsetZ);

    if (spawnPos.toVector3i().equals(blockPos)) {
      spawnPos.add(Vector3i.LEFT);
    }

    return spawnPos;
  }

  private Ref<EntityStore> spawnNpc(
      Store<EntityStore> entityStore,
      Vector3d position,
      Vector3f rotation,
      int npcCount) {
    Pair<Ref<EntityStore>, INonPlayerCharacter> result = NPCPlugin.get()
        .spawnNPC(
            entityStore,
            SPAWNABLE_NPC[npcCount % SPAWNABLE_NPC.length],
            null,
            position,
            rotation);

    assert result != null;

    Ref<EntityStore> npcRef = result.key();
    return npcRef;
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
