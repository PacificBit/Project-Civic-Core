package com.pacificbytestudios.projectciviccore.village.systems.heart;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.util.FastRandom;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.codecs.AreaFootprint;
import com.pacificbytestudios.projectciviccore.codecs.BlockContext;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageBuilderComponent;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartConfigComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageSpawnComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnVillagerTask;
import com.pacificbytestudios.projectciviccore.village.enums.VillageTopologyStructureType;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;
import com.pacificbytestudios.projectciviccore.villagers.enums.VillagerRole;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class VillageHeartPlacementSystem extends RefSystem<ChunkStore> {
  private static final VillagerRole[] SPAWNABLE_NPC = VillagerRole.values();
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  @Override
  public void onEntityAdded(
      Ref<ChunkStore> ref,
      AddReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    Utils.BlockContext context = Utils.getWorldBlockContext(ref, commandBuffer);
    if (context == null) {
      context = Utils.getWorldBlockContext(ref, store);
    }
    if (context == null) {
      return;
    }

    WorldChunk chunk = context.chunk();
    World world = chunk.getWorld();
    Vec3i villageHeartPos = context.position();

    VillageHeartComponent villageHeart = store.getComponent(ref,
        VillageHeartComponent.getComponentType());
    if (villageHeart == null) {
      return;
    }

    String worldName = world.getName();
    villageHeart.setBlockContext(new BlockContext(worldName, chunk.getIndex(), Vec3i.from(villageHeartPos)));
    villageHeart.setVillageRef(new EntityRef(worldName, Vec3i.from(villageHeartPos)));

    VillageSpawnComponent spawnComponent = villageHeart.getComponent(
        VillageSpawnComponent.getComponentType());
    VillageTopologyComponent topology = villageHeart.getComponent(
        VillageTopologyComponent.getComponentType());
    if (spawnComponent == null || topology == null) {
      return;
    }

    // Generate and set random Village name
    VillageHeartConfigComponent heartConfig = villageHeart.getConfig();
    String villageName = this.generateRandomVillageName(
        heartConfig.getFirstVillageNameComponents(), heartConfig.getSecondVillageNameComponents());
    villageHeart.setName(villageName);
    Logger.logInfo("Generated Village name: " + villageHeart.getName());

    int radius = spawnComponent.getSpawnNpcRange();
    if (villageHeart.getUuid() == null) {
      villageHeart.setUuid(UUID.randomUUID());
    }

    Rotation topologyRotation = Rotation.None;
    Vec3i prefabAnchor = Vec3i.from(villageHeartPos).add(Vec3i.safeDown());

    TrackedPrefabPlacementComponent trackedPlacement = villageHeart.getComponent(
        TrackedPrefabPlacementComponent.getComponentType());
    if (trackedPlacement != null) {
      prefabAnchor = trackedPlacement.getOrigin();
      topologyRotation = trackedPlacement.getYaw();
    }

    topology.setSeed(new FastRandom().nextLong(Long.MIN_VALUE, Long.MAX_VALUE));
    topology.setVillageOrientation(topologyRotation);

    short claimRadius = villageHeart.getConfig().getClaimRadiusPerLevel((short) villageHeart.getLevel());
    TopologyStructure coreStructure = new TopologyStructure(
        Vec3i.from(prefabAnchor),
        StructureConfig.TownHeart.SIZE_2I,
        VillageTopologyStructureType.CITY_CENTER,
        topologyRotation);
    coreStructure.setPrefabAnchor(Vec3i.from(prefabAnchor));
    coreStructure.setStructureId(villageHeart.getUuid());
    topology.initializeVillageTopology(coreStructure, claimRadius);

    VillageBuilderComponent builderComponent = commandBuffer.ensureAndGetComponent(
        ref,
        VillageBuilderComponent.getComponentType());
    builderComponent.setProjectPlan(spawnComponent.getStructureToSpawn());
    builderComponent.setFirstVillageBuildProject(true);

    VillageTaskQueueComponent taskComponent = commandBuffer.ensureAndGetComponent(
        ref,
        VillageTaskQueueComponent.getComponentType());
    for (int npcCount = 0; npcCount < spawnComponent.getInitialNpcCount(); npcCount++) {
      String npcRole = SPAWNABLE_NPC[npcCount % SPAWNABLE_NPC.length].getRole();
      taskComponent.registerTask(new SpawnVillagerTask(npcRole, radius));
    }

    AreaFootprint area = villageHeart.getClaimedArea();
    if (area == null) {
      area = Utils.claimVillageArea(villageHeart, villageHeartPos);
    }

    villageStore.registerVillage(
        villageHeart.getUuid(),
        worldName,
        villageHeartPos,
        area);

    commandBuffer.removeComponent(ref, VillageSpawnComponent.getComponentType());
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
        VillageSpawnComponent.getComponentType(),
        VillageTopologyComponent.getComponentType(),
        BlockModule.BlockStateInfo.getComponentType());
  }

  private String generateRandomVillageName(String[] firstComponents, String[] secondComponents) {
    int randomFirstComponentIdx = ThreadLocalRandom.current().nextInt(firstComponents.length);
    String firstComponent = firstComponents[randomFirstComponentIdx];

    int randomSecondComponentIdx = ThreadLocalRandom.current().nextInt(secondComponents.length);
    String secondComponent = secondComponents[randomSecondComponentIdx];

    return firstComponent + " " + secondComponent;
  }
}
