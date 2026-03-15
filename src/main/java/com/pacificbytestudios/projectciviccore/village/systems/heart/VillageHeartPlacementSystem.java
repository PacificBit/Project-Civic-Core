package com.pacificbytestudios.projectciviccore.village.systems.heart;

import java.util.UUID;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.Utils.BlockContext;
import com.pacificbytestudios.projectciviccore.village.components.beds.VillageBedComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageSpawnComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnVillagerTask;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

public class VillageHeartPlacementSystem extends RefSystem<ChunkStore> {
  private static final String VILLAGER_BED_ID = "Furniture_Crude_Villager_Bed";
  private static final String[] SPAWNABLE_NPC = new String[] {
      "Female_Civilian",
      "Male_Civilian"
  };

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
      VillageBedComponent[] beds = spawnBeds(world, villageHeartPos, spawnComponent.getInitialNpcCount(), radius);

      VillageTaskQueueComponent taskComponent = new VillageTaskQueueComponent();
      for (int npcCount = 0; npcCount < spawnComponent.getInitialNpcCount(); npcCount++) {
        String npcRole = SPAWNABLE_NPC[npcCount % SPAWNABLE_NPC.length];
        taskComponent.registerTask(new SpawnVillagerTask(npcRole, radius, beds[npcCount]));
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

  private VillageBedComponent[] spawnBeds(World world, Vector3i villageCenter, int villagerCount, int radius) {
    VillageBedComponent[] beds = new VillageBedComponent[villagerCount];
    BlockType villagerBed = BlockType.getAssetMap().getAsset(VILLAGER_BED_ID);
    int sides = 4;
    int bedsPerSide = (int) Math.ceil((double) villagerCount / sides);
    if (bedsPerSide <= 0) {
      bedsPerSide = 1;
    }

    int sideSpan = Math.max(1, bedsPerSide);
    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(villageCenter.x, villageCenter.z));
    Store<ChunkStore> chunkStore = chunk.getReference().getStore();

    for (int i = 0; i < villagerCount; i++) {
      int sideIndex = (i / bedsPerSide) % sides;
      int indexOnSide = i % bedsPerSide;

      Vector3i outward = Vector3i.CARDINAL_DIRECTIONS[sideIndex].clone().scale(radius);
      Vector3i tangent = Vector3i.CARDINAL_DIRECTIONS[(sideIndex + 1) % sides].clone();

      int halfSpan = sideSpan / 2;
      int tangentOffset = indexOnSide - halfSpan;

      Vector3i pos = villageCenter.clone()
          .add(outward)
          .add(tangent.clone().scale(tangentOffset));

      if (!ChunkUtil.isInsideChunk(chunk.getX(), chunk.getZ(), pos.x, pos.z)) {
        chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
        chunkStore = chunk.getReference().getStore();
      }

      chunk.setBlock(pos.x, pos.y, pos.z, villagerBed);
      Ref<ChunkStore> bedRef = chunk.getBlockComponentEntity(pos.x, pos.y, pos.z);
      VillageBedComponent bedComponent = null;

      if (bedRef != null && bedRef.isValid()) {
        bedComponent = chunkStore.getComponent(bedRef, VillageBedComponent.getComponentType());
      }

      if (bedComponent == null) {
        ProjectCivicCore.LOGGER.atWarning()
            .log("[VillageHeartPlacementSystem] Missing bed block entity at " + pos
                + ". Falling back to an in-memory bed component.");
        bedComponent = new VillageBedComponent();
      }

      bedComponent.setPosition(pos);
      beds[i] = bedComponent;
    }

    return beds;
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
