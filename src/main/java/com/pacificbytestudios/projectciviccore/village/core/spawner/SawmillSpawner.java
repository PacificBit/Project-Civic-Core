package com.pacificbytestudios.projectciviccore.village.core.spawner;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.villagers.enums.WorkType;

import io.sentry.util.Pair;

public class SawmillSpawner {
  public static boolean spawn(WorldChunk chunk, Vector3i position, VillageHeartComponent heart) {
    return spawn(chunk, position, Rotation.None, heart);
  }

  public static boolean spawn(
      WorldChunk chunk,
      Vector3i position,
      Rotation rotation,
      VillageHeartComponent heart) {
    Rotation structureRotation = StructurePlacementUtils.normalize(rotation);
    World world = chunk.getWorld();
    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        StructureConfig.Sawmill.PREFAB_JSON,
        position.clone(),
        structureRotation,
        true);

    if (trackedPlacement == null) {
      return false;
    }

    Vector3i workplaceBlockPosition = getWorkplaceBlockPosition(position, structureRotation);
    WorldChunk workplaceChunk = Utils.setBlockChunkAware(
        chunk,
        workplaceBlockPosition,
        StructureConfig.Sawmill.WORKPLACE_BLOCK,
        StructurePlacementUtils.toBlockRotationIndex(structureRotation));
    Ref<ChunkStore> workplace = workplaceChunk.getBlockComponentEntity(
        workplaceBlockPosition.x,
        workplaceBlockPosition.y,
        workplaceBlockPosition.z);

    if (workplace == null || !workplace.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[SawmillSpawner - spawn] Workplace ref is invalid");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement, workplaceBlockPosition);
    }

    Store<ChunkStore> chunkStore = workplace.getStore();
    WorkplaceComponent workplaceComponent = chunkStore.getComponent(
        workplace,
        WorkplaceComponent.getComponentType());

    if (workplaceComponent == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[SawmillSpawner - spawn] Workplace component is missing");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement, workplaceBlockPosition);
    }

    if (workplaceComponent.getUuid() == null) {
      workplaceComponent.setUuid(UUID.randomUUID());
    }
    workplaceComponent.setLevel((short) 1);
    workplaceComponent.setWorkerType(new WorkType[] { WorkType.WOOD_CUTTER });
    workplaceComponent.setWorkplaceRef(new EntityRef(chunk.getWorld().getName(), workplaceBlockPosition));
    chunkStore.putComponent(
        workplace,
        TrackedPrefabPlacementComponent.getComponentType(),
        trackedPlacement);

    workplaceComponent.setPrefabAnchor(position.clone());

    chunk.markNeedsSaving();
    return true;
  }

  public static boolean despawn(WorldChunk chunk, Vector3i position) {
    Vector3i workplaceBlockPosition = position.clone();
    Pair<WorldChunk, Ref<ChunkStore>> result = Utils.getBlockComponentEntityChunkAware(
        chunk,
        workplaceBlockPosition);

    WorldChunk workplaceChunk = result.getFirst();
    Ref<ChunkStore> workplaceRef = result.getSecond();

    if (workplaceRef == null || !workplaceRef.isValid()) {
      workplaceBlockPosition = getWorkplaceBlockPosition(position, Rotation.None);
      result = Utils.getBlockComponentEntityChunkAware(chunk, workplaceBlockPosition);
      workplaceChunk = result.getFirst();
      workplaceRef = result.getSecond();
    }

    if (workplaceRef == null || !workplaceRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[SawmillSpawner - despawn] Invalid workplaceRef");
      return false;
    }

    Store<ChunkStore> store = workplaceRef.getStore();

    WorkplaceComponent component = store.getComponent(
        workplaceRef,
        WorkplaceComponent.getComponentType());
    TrackedPrefabPlacementComponent trackedPlacement = store.getComponent(
        workplaceRef,
        TrackedPrefabPlacementComponent.getComponentType());

    if (component == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[SawmillSpawner - despawn] Missing workplace component");
      return false;
    }

    Rotation prefabRotation = trackedPlacement != null ? trackedPlacement.getYaw() : Rotation.None;
    boolean despawn = false;
    if (trackedPlacement != null && trackedPlacement.hasRestoreSnapshot()) {
      despawn = PrefabManager.restoreTrackedPrefab(workplaceChunk.getWorld(), trackedPlacement);
    } else {
      Vector3i prefabAnchor = component.getPrefabAnchor();
      if (prefabAnchor == null && trackedPlacement != null) {
        prefabAnchor = trackedPlacement.getOrigin();
      }
      if (prefabAnchor == null) {
        prefabAnchor = getPrefabAnchorFromWorkplace(workplaceBlockPosition, prefabRotation);
      }

      despawn = PrefabManager.removePrefab(
          workplaceChunk.getWorld(),
          StructureConfig.Sawmill.PREFAB_JSON,
          prefabAnchor,
          prefabRotation,
          true);
    }

    if (!despawn) {
      ProjectCivicCore.LOGGER.atSevere().log("[SawmillSpawner - despawn] Could not despawn sawmill prefab");
      return false;
    }

    Utils.breakBlockChunkAware(workplaceChunk, workplaceBlockPosition);

    return true;
  }

  private static Vector3i getWorkplaceBlockPosition(Vector3i prefabAnchor, Rotation rotation) {
    return StructurePlacementUtils.withOffset(
        prefabAnchor,
        StructureConfig.Sawmill.WORKPLACE_BLOCK_OFFSET,
        rotation);
  }

  private static Vector3i getPrefabAnchorFromWorkplace(Vector3i workplaceBlockPosition, Rotation rotation) {
    return StructurePlacementUtils.removeOffset(
        workplaceBlockPosition,
        StructureConfig.Sawmill.WORKPLACE_BLOCK_OFFSET,
        rotation);
  }

  private static boolean rollbackFailedSpawn(
      World world,
      WorldChunk chunk,
      TrackedPrefabPlacementComponent trackedPlacement,
      Vector3i workplaceBlockPosition) {
    Utils.breakBlockChunkAware(chunk, workplaceBlockPosition);
    PrefabManager.restoreTrackedPrefab(world, trackedPlacement);
    return false;
  }
}
