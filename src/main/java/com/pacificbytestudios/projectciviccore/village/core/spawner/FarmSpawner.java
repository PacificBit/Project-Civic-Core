
package com.pacificbytestudios.projectciviccore.village.core.spawner;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.WorkplaceStorageComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.WorkplaceStorageContainerComponent;

import io.sentry.util.Pair;

public class FarmSpawner {
  private static final String PREFAB_JSON = "Structures/WorkPlace/Workspace_Farm.prefab.json";
  private static final BlockType WORKPLACE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Farm_Core");
  private static final BlockType SINGLE_STORAGE_BLOCK = BlockType.getAssetMap().getAsset("Workplace_Chest_Small");
  private static final ClaimedAreaComponent WORKING_AREA_OFFSET = new ClaimedAreaComponent(
      new Vector2i(-3, -3),
      new Vector2i(3, 3));
  // private static final BlockType DOUBLE_STORAGE_BLOCK =
  // BlockType.getAssetMap().getAsset("Workplace_Chest_Large");

  public static boolean spawn(WorldChunk chunk, Vector3i position) {
    World world = chunk.getWorld();
    TrackedPrefabPlacementComponent trackedPlacement = PrefabManager.spawnTrackedPrefab(
        world,
        PREFAB_JSON,
        position.clone(),
        Rotation.None,
        true);
    if (trackedPlacement == null) {
      return false;
    }

    Vector3i workplaceBlockPosition = getWorkplaceBlockPosition(position);
    WorldChunk workplaceChunk = Utils.setBlockChunkAware(chunk, workplaceBlockPosition, WORKPLACE_BLOCK);
    Ref<ChunkStore> workplace = workplaceChunk.getBlockComponentEntity(
        workplaceBlockPosition.x,
        workplaceBlockPosition.y,
        workplaceBlockPosition.z);

    if (workplace == null || !workplace.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[FarmSpawner - spawn] Workplace ref is invalid");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement, workplaceBlockPosition, null);
    }

    Store<ChunkStore> workplaceStore = workplace.getStore();
    WorkplaceComponent workplaceComponent = workplaceStore.getComponent(
        workplace,
        WorkplaceComponent.getComponentType());

    if (workplaceComponent == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[FarmSpawner - spawn] Workplace component is missing");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement, workplaceBlockPosition, null);
    }

    workplaceComponent.setWorkingArea(new ClaimedAreaComponent(WORKING_AREA_OFFSET, position));
    workplaceComponent.setWorkplaceRef(new EntityRef(chunk.getWorld().getName(), workplaceBlockPosition));

    workplaceStore.putComponent(
        workplace,
        TrackedPrefabPlacementComponent.getComponentType(),
        trackedPlacement);

    Vector3i containerPosition = getContainerPosition(workplaceBlockPosition);
    WorldChunk containerChunk = Utils.setBlockChunkAware(workplaceChunk, containerPosition, SINGLE_STORAGE_BLOCK);
    Ref<ChunkStore> containerRef = containerChunk.getBlockComponentEntity(containerPosition.x,
        containerPosition.y, containerPosition.z);

    if (containerRef == null || !containerRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[FarmSpawner - spawn] Container ref is invalid");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement, workplaceBlockPosition, containerPosition);
    }

    Store<ChunkStore> containerStore = containerRef.getStore();
    WorkplaceStorageContainerComponent container = containerStore.getComponent(
        containerRef,
        WorkplaceStorageContainerComponent.getComponentType());

    if (container == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[FarmSpawner - spawn] Container component is missing");
      return rollbackFailedSpawn(world, workplaceChunk, trackedPlacement, workplaceBlockPosition, containerPosition);
    }

    workplaceComponent.setPrefabAnchor(position.clone());

    container.setPosition(containerPosition);
    container.setUuid(UUID.randomUUID());

    WorkplaceStorageComponent storage = new WorkplaceStorageComponent();
    storage.registerContainer(container.getUuid(), container);
    workplaceComponent.setStorage(storage);

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
      ProjectCivicCore.LOGGER.atSevere().log("[FarmSpawner - despawn] Invalid workplaceRef");
      return false;
    }

    Store<ChunkStore> store = workplaceRef.getStore();

    WorkplaceComponent component = store.getComponent(workplaceRef,
        WorkplaceComponent.getComponentType());
    TrackedPrefabPlacementComponent trackedPlacement = store
        .getComponent(workplaceRef, TrackedPrefabPlacementComponent.getComponentType());

    if (component == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[FarmSpawner - despawn] Missing workplace component");
      return false;
    }

    boolean despawn = false;
    if (trackedPlacement != null && trackedPlacement.hasRestoreSnapshot()) {
      despawn = PrefabManager.restoreTrackedPrefab(workplaceChunk.getWorld(), trackedPlacement);
    } else {
      Vector3i prefabAnchor = component.getPrefabAnchor();
      if (prefabAnchor == null && trackedPlacement != null) {
        prefabAnchor = trackedPlacement.getOrigin();
      }
      if (prefabAnchor == null) {
        prefabAnchor = getPrefabAnchorFromWorkplace(workplaceBlockPosition);
      }

      despawn = PrefabManager.removePrefab(
          workplaceChunk.getWorld(),
          PREFAB_JSON,
          prefabAnchor,
          Rotation.None,
          true);
    }

    if (!despawn) {
      ProjectCivicCore.LOGGER.atSevere().log("[FarmSpawner - despawn] Could not despawn farm prefab");
      return false;
    }

    Vector3i containerPosition = getContainerPosition(workplaceBlockPosition);
    Utils.breakBlockChunkAware(workplaceChunk, workplaceBlockPosition);
    Utils.breakBlockChunkAware(workplaceChunk, containerPosition);

    return despawn;
  }

  private static Vector3i getWorkplaceBlockPosition(Vector3i prefabAnchor) {
    return prefabAnchor.clone().add(Vector3i.NORTH.clone().scale(4)).add(Vector3i.UP);
  }

  private static Vector3i getContainerPosition(Vector3i workplaceBlockPosition) {
    return workplaceBlockPosition.clone().add(Vector3i.EAST.clone().scale(2));
  }

  private static Vector3i getPrefabAnchorFromWorkplace(Vector3i workplaceBlockPosition) {
    return workplaceBlockPosition.clone().add(Vector3i.SOUTH.clone().scale(4)).add(Vector3i.DOWN);
  }

  private static boolean rollbackFailedSpawn(
      World world,
      WorldChunk chunk,
      TrackedPrefabPlacementComponent trackedPlacement,
      Vector3i workplaceBlockPosition,
      Vector3i containerPosition) {
    Utils.breakBlockChunkAware(chunk, workplaceBlockPosition);
    if (containerPosition != null) {
      Utils.breakBlockChunkAware(chunk, containerPosition);
    }
    PrefabManager.restoreTrackedPrefab(world, trackedPlacement);
    return false;
  }
}
