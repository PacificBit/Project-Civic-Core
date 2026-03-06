package com.pacificbytestudios.projectciviccore.village.systems.heart;

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
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.Utils.BlockContext;
import com.pacificbytestudios.projectciviccore.village.codec.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.village.components.beds.VillageBedComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageSpawnComponent;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;
import com.pacificbytestudios.projectciviccore.villagers.codecs.VillageHeartRef;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;

import it.unimi.dsi.fastutil.Pair;

public class VillageHeartPlacementSystem extends RefSystem<ChunkStore> {
  private static final String VILLAGER_BED_ID = "Furniture_Crude_Villager_Bed";
  private static final String[] SPAWNABLE_NPC = new String[] {
      "Female_Unemployed_Civilian",
      "Male_Unemployed_Civilian"
  };

  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();
  private FastRandom random = new FastRandom();

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
    VillageHeartComponent villageHeartComponent = store.getComponent(ref,
        VillageHeartComponent.getComponentType());
    int radius = spawnComponent.getSpawnNpcRange();

    VillageHeartRef villageHeartLocation = new VillageHeartRef(world.getName(), villageHeartPos);

    ClaimedAreaComponent area = Utils.claimVillageArea(villageHeartComponent, villageHeartPos);
    villageStore.registerVillage(
        world.getName(),
        villageHeartPos,
        area);

    world.execute(() -> {
      Store<EntityStore> entityStore = world.getEntityStore().getStore();

      Vector3f rotation = new Vector3f(0f, 0f, 0f);

      VillageBedComponent[] beds = spawnBeds(world, villageHeartPos, spawnComponent.getInitialNpcCount(), radius);

      for (int npcCount = 0; npcCount < spawnComponent.getInitialNpcCount(); npcCount++) {
        Vector3d spawnPos = newSpawnPos(villageHeartPos, radius);
        Ref<EntityStore> npcRef = spawnNpc(entityStore, spawnPos, rotation, npcCount);

        assert npcRef != null;

        UUIDComponent uuidComponent = entityStore.getComponent(npcRef, UUIDComponent.getComponentType());
        villageHeartComponent.registerNpc(uuidComponent.getUuid());

        beds[npcCount].setOwner(uuidComponent.getUuid());
        beds[npcCount].setTaken(true);

        VillagerComponent villagerComponent = new VillagerComponent();
        villagerComponent.setVillageHeartLocation(villageHeartLocation.clone());
        villagerComponent.setBedPosition(beds[npcCount].getPosition());
        entityStore.putComponent(npcRef, VillagerComponent.getComponentType(), villagerComponent);
      }

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
      VillageBedComponent bedComponent = chunkStore.getComponent(bedRef, VillageBedComponent.getComponentType());
      bedComponent.setPosition(pos);
      beds[i] = bedComponent;
    }

    return beds;
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
