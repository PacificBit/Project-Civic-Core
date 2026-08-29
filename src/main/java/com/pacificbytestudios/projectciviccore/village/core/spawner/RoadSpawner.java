package com.pacificbytestudios.projectciviccore.village.core.spawner;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.modules.block.BlockEntity;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockComponentSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig.Road.RoadLength;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig.Road.RoadType;

import io.sentry.util.Pair;

public class RoadSpawner {
  public static boolean spawn(
      WorldChunk chunk,
      Vec3i position,
      Rotation rotation,
      VillageHeartComponent village,
      RoadType type,
      RoadLength length) {
    String prefab = StructureConfig.Road.getRoadPrefab((short) village.getLevel(), type, length);
    Rotation structureRotation = StructurePlacementUtils.normalize(rotation);

    if (prefab == null) {
      Logger.logWarning(
          "Missing road prefab for villageLevel=%d, type=%s, length=%s",
          village.getLevel(), type, length);
      return false;
    }

    World world = chunk.getWorld();
    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        prefab,
        position.clone(),
        structureRotation,
        true);

    if (trackedPlacement == null) {
      return false;
    }

    Vec3i anchorBlockPosition = position.clone();
    Pair<WorldChunk, Ref<ChunkStore>> anchorResult = Utils.getBlockComponentEntityChunkAware(
        chunk,
        anchorBlockPosition);
    WorldChunk anchorChunk = anchorResult.getFirst();
    Holder<ChunkStore> anchorHolder = createAnchorHolder(anchorChunk, anchorBlockPosition);

    anchorHolder.putComponent(
        TrackedPrefabPlacementComponent.getComponentType(),
        trackedPlacement);

    BlockType anchorBlockType = world.getBlockType(
        anchorBlockPosition.x,
        anchorBlockPosition.y,
        anchorBlockPosition.z);
    int anchorRotation = StructurePlacementUtils.toBlockRotationIndex(structureRotation);
    ChunkStore worldChunkStore = anchorChunk.getWorld().getChunkStore();
    Store<ChunkStore> chunkStore = worldChunkStore.getStore();
    Ref<ChunkStore> sectionRef = worldChunkStore.getChunkSectionReferenceAtBlock(
        anchorBlockPosition.x,
        anchorBlockPosition.y,
        anchorBlockPosition.z);
    BlockComponentSection blockComponentSection = sectionRef == null
        ? null
        : chunkStore.getComponent(sectionRef, BlockComponentSection.getComponentType());
    if (sectionRef == null || !sectionRef.isValid() || blockComponentSection == null) {
      Logger.logWarning("Could not resolve anchor block section at %s".formatted(anchorBlockPosition));
      return rollbackFailedSpawn(world, trackedPlacement);
    }

    BlockEntity.setBlockEntity(
        chunkStore,
        sectionRef,
        blockComponentSection,
        anchorBlockPosition.x,
        anchorBlockPosition.y,
        anchorBlockPosition.z,
        anchorBlockType,
        anchorRotation,
        anchorHolder);

    Ref<ChunkStore> anchorRef = Utils.getBlockComponentEntity(world, anchorBlockPosition);

    if (anchorRef == null || !anchorRef.isValid()) {
      Logger.logWarning(
          "Anchor ref is invalid at %s".formatted(anchorBlockPosition));
      return rollbackFailedSpawn(world, trackedPlacement);
    }

    chunkStore.putComponent(
        anchorRef,
        TrackedPrefabPlacementComponent.getComponentType(),
        trackedPlacement);

    anchorChunk.markNeedsSaving();
    return true;
  }

  public static boolean despawn(WorldChunk chunk, Vec3i position) {
    WorldChunk anchorChunk = chunk;
    Ref<ChunkStore> anchorRef = Utils.getBlockComponentEntity(anchorChunk.getWorld(), position);
    if (anchorRef == null || !anchorRef.isValid()) {
      Logger.logWarning("Invalid anchor ref");
      return false;
    }

    Store<ChunkStore> store = anchorRef.getStore();
    TrackedPrefabPlacementComponent trackedPlacement = store.getComponent(
        anchorRef,
        TrackedPrefabPlacementComponent.getComponentType());

    if (trackedPlacement == null) {
      Logger.logWarning("Missing tracked prefab metadata");
      return false;
    }

    boolean despawn = trackedPlacement.hasRestoreSnapshot()
        ? PrefabManager.restoreTrackedPrefab(anchorChunk.getWorld(), trackedPlacement)
        : PrefabManager.removePrefab(
            anchorChunk.getWorld(),
            trackedPlacement.getPrefabPath(),
            trackedPlacement.getOrigin(),
            trackedPlacement.getYaw(),
            true);

    if (!despawn) {
      Logger.logWarning("Could not despawn road prefab");
      return false;
    }

    anchorChunk.markNeedsSaving();
    return true;
  }

  private static Holder<ChunkStore> createAnchorHolder(WorldChunk chunk, Vec3i position) {
    Holder<ChunkStore> holder = chunk.getWorld().getBlockComponentHolder(
        position.x,
        position.y,
        position.z);
    return holder != null ? holder.clone() : ChunkStore.REGISTRY.newHolder();
  }

  private static boolean rollbackFailedSpawn(
      World world,
      TrackedPrefabPlacementComponent trackedPlacement) {
    PrefabManager.restoreTrackedPrefab(world, trackedPlacement);
    return false;
  }

}
