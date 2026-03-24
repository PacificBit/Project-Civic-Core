package com.pacificbytestudios.projectciviccore.village.core.spawner;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.codecs.BlockContext;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageBuilderComponent;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageSpawnComponent;
import com.pacificbytestudios.projectciviccore.village.enums.VillageTopologyStructureType;

public class TownHeartSpawner {

  public static boolean spawn(WorldChunk chunk, Vector3i position) {
    return spawn(chunk, position, Rotation.None);
  }

  public static boolean spawn(WorldChunk chunk, Vector3i position, Rotation rotation) {
    Rotation structureRotation = StructurePlacementUtils.normalize(rotation);
    World world = chunk.getWorld();
    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        StructureConfig.TownHeart.PREFAB_JSON,
        position.clone(),
        structureRotation,
        true);
    if (trackedPlacement == null) {
      return false;
    }

    WorldChunk heartBlockChunk = chunk;
    Vector3i heartBlockPosition = position.clone().add(Vector3i.UP);
    heartBlockChunk = Utils.setBlockChunkAware(heartBlockChunk, heartBlockPosition,
        StructureConfig.TownHeart.WORKPLACE_BLOCK);

    Ref<ChunkStore> heartRef = heartBlockChunk.getBlockComponentEntity(position.x, position.y + 1, position.z);
    if (heartRef == null || !heartRef.isValid()) {
      Utils.breakBlockChunkAware(heartBlockChunk, heartBlockPosition);
      PrefabManager.restoreTrackedPrefab(world, trackedPlacement);
      return false;
    }

    Store<ChunkStore> store = heartRef.getStore();
    VillageHeartComponent heartComponent = store.getComponent(heartRef,
        VillageHeartComponent.getComponentType());

    String worldName = heartBlockChunk.getWorld().getName();
    heartComponent.setBlockContext(new BlockContext(worldName, heartBlockChunk.getIndex(), heartBlockPosition));
    heartComponent.setVillageRef(new EntityRef(worldName, position));

    VillageSpawnComponent spawnComponent = store.getComponent(heartRef, VillageSpawnComponent.getComponentType());

    initializeVillageTopology(position, structureRotation, heartRef, store, heartComponent);

    store.putComponent(
        heartRef,
        TrackedPrefabPlacementComponent.getComponentType(),
        trackedPlacement);

    VillageBuilderComponent builderComponent = new VillageBuilderComponent();
    builderComponent.setProjectPlan(spawnComponent.getStructureToSpawn());
    builderComponent.setFirstVillageBuildProject(true);

    store.putComponent(
        heartRef,
        VillageBuilderComponent.getComponentType(),
        builderComponent);

    return true;
  }

  private static void initializeVillageTopology(
      Vector3i position,
      Rotation rotation,
      Ref<ChunkStore> heartRef,
      Store<ChunkStore> store,
      VillageHeartComponent heartComponent) {
    VillageTopologyComponent topologyComponent = store.getComponent(heartRef,
        VillageTopologyComponent.getComponentType());
    topologyComponent.setVillageOrientation(rotation);

    short radius = heartComponent.getConfig().getClaimRadiusPerLevel((short) heartComponent.getLevel());

    TopologyStructure coreStructure = new TopologyStructure(
        position,
        StructureConfig.TownHeart.SIZE_2I,
        VillageTopologyStructureType.CITY_CENTER,
        rotation);
    coreStructure.setStructureId(heartComponent.getUuid());

    topologyComponent.initializeVillageTopology(
        coreStructure,
        radius);
  }

  public static boolean despawn(WorldChunk chunk, Vector3i position) {
    return despawn(chunk, position, Rotation.None);
  }

  public static boolean despawn(WorldChunk chunk, Vector3i position, Rotation rotation) {
    return PrefabManager.removePrefab(
        chunk.getWorld(),
        StructureConfig.TownHeart.PREFAB_JSON,
        position.clone(),
        StructurePlacementUtils.normalize(rotation),
        true);
  }
}
