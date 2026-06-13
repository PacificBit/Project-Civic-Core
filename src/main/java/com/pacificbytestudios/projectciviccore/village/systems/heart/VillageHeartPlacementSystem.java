package com.pacificbytestudios.projectciviccore.village.systems.heart;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageBuilderComponent;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartConfigComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartDebugComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageSpawnComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnVillagerTask;
import com.pacificbytestudios.projectciviccore.village.enums.VillageTopologyStructureType;
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
    BlockContext context = this.getPlacementContext(ref, store, commandBuffer);
    if (context == null) {
      return;
    }

    WorldChunk chunk = Utils.getWorldChunk(context);
    if (chunk == null) {
      return;
    }
    World world = chunk.getWorld();
    Vec3i villageHeartPos = context.getPosition();

    VillageHeartComponent villageHeart = this.getVillageHeart(ref, store);
    if (villageHeart == null) {
      return;
    }

    String worldName = world.getName();
    this.setVillagePlacementIdentity(villageHeart, worldName, chunk, villageHeartPos);

    VillageSpawnComponent spawnComponent = villageHeart.getComponent(
        VillageSpawnComponent.getComponentType());
    VillageTopologyComponent topology = villageHeart.getComponent(
        VillageTopologyComponent.getComponentType());
    if (spawnComponent == null || topology == null) {
      return;
    }

    VillageHeartConfigComponent heartConfig = villageHeart.getConfig();
    this.generateAndSetVillageName(villageHeart, heartConfig);
    this.addDebugComponentIfConfigured(ref, commandBuffer, heartConfig);
    this.ensureVillageUuid(villageHeart);

    VillageHeartPlacement placement = this.getHeartPlacement(villageHeart, villageHeartPos);
    this.initializeVillageTopology(villageHeart, topology, placement);

    this.prepareFirstBuildProject(ref, commandBuffer, spawnComponent);
    this.queueInitialVillagers(ref, commandBuffer, spawnComponent);
    this.registerClaimedArea(villageHeart, worldName, villageHeartPos);

    commandBuffer.removeComponent(ref, VillageSpawnComponent.getComponentType());
  }

  private BlockContext getPlacementContext(
      Ref<ChunkStore> ref,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    BlockContext context = Utils.getWorldBlockContext(ref, commandBuffer);
    if (context == null) {
      context = Utils.getWorldBlockContext(ref, store);
    }
    return context;
  }

  private VillageHeartComponent getVillageHeart(Ref<ChunkStore> ref, Store<ChunkStore> store) {
    return store.getComponent(ref, VillageHeartComponent.getComponentType());
  }

  private void setVillagePlacementIdentity(
      VillageHeartComponent villageHeart,
      String worldName,
      WorldChunk chunk,
      Vec3i villageHeartPos) {
    villageHeart.setBlockContext(new BlockContext(
        worldName,
        chunk.getIndex(),
        Vec3i.from(villageHeartPos)));
    villageHeart.setVillageRef(new EntityRef(worldName, Vec3i.from(villageHeartPos)));
  }

  private void generateAndSetVillageName(
      VillageHeartComponent villageHeart,
      VillageHeartConfigComponent heartConfig) {
    String villageName = this.generateRandomVillageName(
        heartConfig.getFirstVillageNameComponents(), heartConfig.getSecondVillageNameComponents());
    villageHeart.setName(villageName);
    Logger.logInfo("Generated Village name: " + villageHeart.getName());
  }

  private void addDebugComponentIfConfigured(
      Ref<ChunkStore> ref,
      CommandBuffer<ChunkStore> commandBuffer,
      VillageHeartConfigComponent heartConfig) {
    if (!heartConfig.isDebugActive()) {
      return;
    }

    commandBuffer.ensureComponent(ref, VillageHeartDebugComponent.getComponentType());
  }

  private void ensureVillageUuid(VillageHeartComponent villageHeart) {
    if (villageHeart.getUuid() == null) {
      villageHeart.setUuid(UUID.randomUUID());
    }
  }

  private VillageHeartPlacement getHeartPlacement(
      VillageHeartComponent villageHeart,
      Vec3i villageHeartPos) {
    TrackedPrefabPlacementComponent trackedPlacement = villageHeart.getComponent(
        TrackedPrefabPlacementComponent.getComponentType());
    if (trackedPlacement != null) {
      return new VillageHeartPlacement(trackedPlacement.getOrigin(), trackedPlacement.getYaw());
    }

    Vec3i prefabAnchor = Vec3i.from(villageHeartPos).add(Vec3i.safeDown());
    return new VillageHeartPlacement(prefabAnchor, Rotation.None);
  }

  private void initializeVillageTopology(
      VillageHeartComponent villageHeart,
      VillageTopologyComponent topology,
      VillageHeartPlacement placement) {
    topology.setSeed(new FastRandom().nextLong(Long.MIN_VALUE, Long.MAX_VALUE));
    topology.setVillageOrientation(placement.getTopologyRotation());

    short claimRadius = villageHeart.getConfig().getClaimRadiusPerLevel(
        (short) villageHeart.getLevel());
    TopologyStructure coreStructure = new TopologyStructure(
        Vec3i.from(placement.getPrefabAnchor()),
        StructureConfig.TownHeart.SIZE_2I,
        VillageTopologyStructureType.CITY_CENTER,
        placement.getTopologyRotation());
    coreStructure.setPrefabAnchor(Vec3i.from(placement.getPrefabAnchor()));
    coreStructure.setStructureId(villageHeart.getUuid());
    topology.initializeVillageTopology(coreStructure, claimRadius);
  }

  private void prepareFirstBuildProject(
      Ref<ChunkStore> ref,
      CommandBuffer<ChunkStore> commandBuffer,
      VillageSpawnComponent spawnComponent) {
    VillageBuilderComponent builderComponent = commandBuffer.ensureAndGetComponent(
        ref,
        VillageBuilderComponent.getComponentType());
    builderComponent.setProjectPlan(spawnComponent.getStructureToSpawn());
    builderComponent.setFirstVillageBuildProject(true);
  }

  private void queueInitialVillagers(
      Ref<ChunkStore> ref,
      CommandBuffer<ChunkStore> commandBuffer,
      VillageSpawnComponent spawnComponent) {
    int radius = spawnComponent.getNpcSpawnRange();

    VillageTaskQueueComponent taskComponent = commandBuffer.ensureAndGetComponent(
        ref,
        VillageTaskQueueComponent.getComponentType());
    for (int npcCount = 0; npcCount < spawnComponent.getInitialNpcCount(); npcCount++) {
      String npcRole = SPAWNABLE_NPC[npcCount % SPAWNABLE_NPC.length].getRole();
      taskComponent.registerTask(new SpawnVillagerTask(npcRole, radius));
    }
  }

  private void registerClaimedArea(
      VillageHeartComponent villageHeart,
      String worldName,
      Vec3i villageHeartPos) {
    AreaFootprint area = villageHeart.getClaimedArea();
    if (area == null) {
      area = Utils.claimVillageArea(villageHeart, villageHeartPos);
    }

    villageStore.registerVillage(
        villageHeart.getUuid(),
        worldName,
        villageHeartPos,
        area);
  }

  private static final class VillageHeartPlacement {
    private final Vec3i prefabAnchor;
    private final Rotation topologyRotation;

    private VillageHeartPlacement(Vec3i prefabAnchor, Rotation topologyRotation) {
      this.prefabAnchor = prefabAnchor;
      this.topologyRotation = topologyRotation;
    }

    private Vec3i getPrefabAnchor() {
      return this.prefabAnchor;
    }

    private Rotation getTopologyRotation() {
      return this.topologyRotation;
    }
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
