package com.pacificbytestudios.projectciviccore.village.components.workplace.storage;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.AVillageTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.EnqueueVillageStorageRequestTask;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public final class VillageStorageRequestInvalidation {
  private VillageStorageRequestInvalidation() {
  }

  public static void invalidateRequesterWorkplace(
      Ref<ChunkStore> villageHeartRef,
      VillageHeartComponent villageHeart,
      UUID requesterWorkplaceId) {
    VillageHeartComponent resolvedHeart = resolveVillageHeart(villageHeartRef, villageHeart);
    if (resolvedHeart == null || requesterWorkplaceId == null) {
      return;
    }

    Set<UUID> affectedRequesterIds = new HashSet<>();
    Set<UUID> affectedStorageWorkerIds = new HashSet<>();
    affectedRequesterIds.add(requesterWorkplaceId);

    removePendingVillageTasks(villageHeartRef, requesterWorkplaceId);

    purgeStorageRequests(
        resolvedHeart,
        request -> isRequestedBy(request, requesterWorkplaceId),
        affectedRequesterIds,
        affectedStorageWorkerIds);

    finalizeInvalidation(villageHeartRef, resolvedHeart, affectedRequesterIds, affectedStorageWorkerIds);
  }

  public static void invalidateTargetStorage(
      Ref<ChunkStore> villageHeartRef,
      VillageHeartComponent villageHeart,
      UUID storageWorkplaceId) {
    VillageHeartComponent resolvedHeart = resolveVillageHeart(villageHeartRef, villageHeart);
    if (resolvedHeart == null || storageWorkplaceId == null) {
      return;
    }

    Set<UUID> affectedRequesterIds = new HashSet<>();
    Set<UUID> affectedStorageWorkerIds = new HashSet<>();
    WorkplaceComponent targetStorage = resolvedHeart.getWorkplaceFromId(storageWorkplaceId);
    if (targetStorage != null) {
      affectedStorageWorkerIds.addAll(targetStorage.getWorkers());
      if (targetStorage.getBossId() != null) {
        affectedStorageWorkerIds.add(targetStorage.getBossId());
      }
    }

    purgeStorageComponentRequests(
        targetStorage,
        affectedRequesterIds,
        affectedStorageWorkerIds);

    finalizeInvalidation(villageHeartRef, resolvedHeart, affectedRequesterIds, affectedStorageWorkerIds);
  }

  private static VillageHeartComponent resolveVillageHeart(
      Ref<ChunkStore> villageHeartRef,
      VillageHeartComponent villageHeart) {
    if (villageHeart != null) {
      return villageHeart;
    }

    if (villageHeartRef == null || !villageHeartRef.isValid()) {
      return null;
    }

    return villageHeartRef.getStore().getComponent(
        villageHeartRef,
        VillageHeartComponent.getComponentType());
  }

  private static boolean removePendingVillageTasks(
      Ref<ChunkStore> villageHeartRef,
      UUID requesterWorkplaceId) {
    if (villageHeartRef == null || !villageHeartRef.isValid() || requesterWorkplaceId == null) {
      return false;
    }

    Store<ChunkStore> heartStore = villageHeartRef.getStore();
    VillageTaskQueueComponent taskQueue = heartStore.getComponent(
        villageHeartRef,
        VillageTaskQueueComponent.getComponentType());
    if (taskQueue == null) {
      return false;
    }

    int removedTasks = taskQueue.removeTasksIf(task -> matchesRequesterVillageTask(task, requesterWorkplaceId));
    if (removedTasks <= 0) {
      return false;
    }

    heartStore.putComponent(villageHeartRef, VillageTaskQueueComponent.getComponentType(), taskQueue);
    markChunkComponentDirty(heartStore, villageHeartRef);

    Logger.logInfo("Removed " + removedTasks
            + " pending storage task(s) for requester workplace " + requesterWorkplaceId);
    return true;
  }

  private static boolean matchesRequesterVillageTask(AVillageTask task, UUID requesterWorkplaceId) {
    if (!(task instanceof EnqueueVillageStorageRequestTask storageTask)) {
      return false;
    }

    return isRequestedBy(storageTask.getRequest(), requesterWorkplaceId);
  }

  private static void purgeStorageRequests(
      VillageHeartComponent villageHeart,
      java.util.function.Predicate<VillageStorageRequest> predicate,
      Set<UUID> affectedRequesterIds,
      Set<UUID> affectedStorageWorkerIds) {
    if (villageHeart == null || predicate == null) {
      return;
    }

    for (UUID storageWorkplaceId : new HashSet<>(villageHeart.getVillageStorageWorkplaces())) {
      WorkplaceComponent storageWorkplace = villageHeart.getWorkplaceFromId(storageWorkplaceId);
      if (storageWorkplace == null || storageWorkplace.getWorkplaceRef() == null) {
        continue;
      }

      Ref<ChunkStore> storageRef = storageWorkplace.getWorkplaceRef().getRef();
      if (storageRef == null || !storageRef.isValid()) {
        continue;
      }

      Store<ChunkStore> storageStore = storageRef.getStore();
      WorkplaceVillageStorageRequestComponent storageRequestComponent = storageStore.getComponent(
          storageRef,
          WorkplaceVillageStorageRequestComponent.getComponentType());
      if (storageRequestComponent == null) {
        continue;
      }

      WorkplaceVillageStorageRequestComponent.PurgeResult purgeResult = storageRequestComponent
          .purgeRequests(predicate);
      if (!purgeResult.hasRemovedRequests()) {
        continue;
      }

      affectedRequesterIds.addAll(purgeResult.getRequesterWorkplaceIds());
      affectedStorageWorkerIds.addAll(purgeResult.getAssignedWorkerIds());

      storageStore.putComponent(
          storageRef,
          WorkplaceVillageStorageRequestComponent.getComponentType(),
          storageRequestComponent);
      markChunkComponentDirty(storageStore, storageRef);
    }
  }

  private static void purgeStorageComponentRequests(
      WorkplaceComponent storageWorkplace,
      Set<UUID> affectedRequesterIds,
      Set<UUID> affectedStorageWorkerIds) {
    if (storageWorkplace == null || storageWorkplace.getWorkplaceRef() == null) {
      return;
    }

    Ref<ChunkStore> storageRef = storageWorkplace.getWorkplaceRef().getRef();
    if (storageRef == null || !storageRef.isValid()) {
      return;
    }

    Store<ChunkStore> storageStore = storageRef.getStore();
    WorkplaceVillageStorageRequestComponent storageRequestComponent = storageStore.getComponent(
        storageRef,
        WorkplaceVillageStorageRequestComponent.getComponentType());
    if (storageRequestComponent == null) {
      return;
    }

    WorkplaceVillageStorageRequestComponent.PurgeResult purgeResult = storageRequestComponent.purgeRequests(
        request -> true);
    if (!purgeResult.hasRemovedRequests()) {
      return;
    }

    affectedRequesterIds.addAll(purgeResult.getRequesterWorkplaceIds());
    affectedStorageWorkerIds.addAll(purgeResult.getAssignedWorkerIds());

    storageStore.putComponent(
        storageRef,
        WorkplaceVillageStorageRequestComponent.getComponentType(),
        storageRequestComponent);
    markChunkComponentDirty(storageStore, storageRef);
  }

  private static void finalizeInvalidation(
      Ref<ChunkStore> villageHeartRef,
      VillageHeartComponent villageHeart,
      Set<UUID> affectedRequesterIds,
      Set<UUID> affectedStorageWorkerIds) {
    if (villageHeartRef == null || !villageHeartRef.isValid() || villageHeart == null) {
      return;
    }

    World world = resolveWorld(villageHeartRef);

    for (UUID requesterWorkplaceId : affectedRequesterIds) {
      clearRequesterLastStorageRequest(world, villageHeart, requesterWorkplaceId);
    }

    for (UUID storageWorkerId : affectedStorageWorkerIds) {
      resetStorageKeeperSnapshot(world, villageHeart, storageWorkerId);
    }

    villageHeartRef.getStore().putComponent(
        villageHeartRef,
        VillageHeartComponent.getComponentType(),
        villageHeart);
    markChunkComponentDirty(villageHeartRef.getStore(), villageHeartRef);

    if (!affectedRequesterIds.isEmpty() || !affectedStorageWorkerIds.isEmpty()) {
      Logger.logInfo("Cleared stale storage links. Requesters="
              + affectedRequesterIds + ", affected storage workers=" + affectedStorageWorkerIds);
    }
  }

  private static void clearRequesterLastStorageRequest(
      World world,
      VillageHeartComponent villageHeart,
      UUID requesterWorkplaceId) {
    WorkplaceComponent registeredWorkplace = villageHeart.getWorkplaceFromId(requesterWorkplaceId);
    if (registeredWorkplace == null) {
      return;
    }

    registeredWorkplace.setLastStorageRequest(null);

    Ref<ChunkStore> requesterRef = registeredWorkplace.getWorkplaceRef() != null
        ? registeredWorkplace.getWorkplaceRef().getRef()
        : null;
    if (requesterRef != null && requesterRef.isValid()) {
      Store<ChunkStore> requesterStore = requesterRef.getStore();
      WorkplaceComponent persistedWorkplace = requesterStore.getComponent(
          requesterRef,
          WorkplaceComponent.getComponentType());
      if (persistedWorkplace != null) {
        persistedWorkplace.setLastStorageRequest(null);
        requesterStore.putComponent(requesterRef, WorkplaceComponent.getComponentType(), persistedWorkplace);
        markChunkComponentDirty(requesterStore, requesterRef);
      }
    }

    for (UUID requesterWorkerId : new HashSet<>(registeredWorkplace.getWorkers())) {
      VillagerComponent heartVillager = villageHeart.getVillager(requesterWorkerId);
      if (heartVillager != null) {
        applyLastStorageRequestClear(heartVillager);
        refreshWorkerWorkplaceSnapshot(heartVillager, villageHeart, false);
      }

      persistVillager(world, requesterWorkerId, villagerComponent -> {
        applyLastStorageRequestClear(villagerComponent);
        refreshWorkerWorkplaceSnapshot(villagerComponent, villageHeart, true);
      });
    }
  }

  private static void resetStorageKeeperSnapshot(
      World world,
      VillageHeartComponent villageHeart,
      UUID storageWorkerId) {
    if (storageWorkerId == null) {
      return;
    }

    VillagerComponent heartVillager = villageHeart.getVillager(storageWorkerId);
    if (heartVillager != null) {
      applySnapshotReset(heartVillager);
      refreshWorkerWorkplaceSnapshot(heartVillager, villageHeart, false);
    }

    persistVillager(world, storageWorkerId, villagerComponent -> {
      applySnapshotReset(villagerComponent);
      refreshWorkerWorkplaceSnapshot(villagerComponent, villageHeart, true);
    });
  }

  private static void persistVillager(
      World world,
      UUID villagerId,
      java.util.function.Consumer<VillagerComponent> mutation) {
    if (world == null || villagerId == null || mutation == null) {
      return;
    }

    Ref<EntityStore> villagerRef = world.getEntityRef(villagerId);
    if (villagerRef == null || !villagerRef.isValid()) {
      return;
    }

    Store<EntityStore> entityStore = world.getEntityStore().getStore();
    VillagerComponent villagerComponent = entityStore.getComponent(
        villagerRef,
        VillagerComponent.getComponentType());
    if (villagerComponent == null) {
      return;
    }

    mutation.accept(villagerComponent);
    entityStore.putComponent(villagerRef, VillagerComponent.getComponentType(), villagerComponent);
  }

  private static void applyLastStorageRequestClear(VillagerComponent villagerComponent) {
    if (villagerComponent == null || villagerComponent.getWorkerData() == null) {
      return;
    }

    WorkplaceComponent workerWorkplace = villagerComponent.getWorkerData().getWorkplace();
    if (workerWorkplace == null) {
      return;
    }

    workerWorkplace.setLastStorageRequest(null);
  }

  private static void applySnapshotReset(VillagerComponent villagerComponent) {
    if (villagerComponent == null) {
      return;
    }

    VillagerWorkerComponent workerData = villagerComponent.getWorkerData();
    if (workerData == null) {
      return;
    }

    workerData.setWorkareaSnapshot(new WorkplaceAreaSnapshot());
  }

  private static void refreshWorkerWorkplaceSnapshot(
      VillagerComponent villagerComponent,
      VillageHeartComponent villageHeart,
      boolean cloneWorkplace) {
    if (villagerComponent == null || villageHeart == null) {
      return;
    }

    VillagerWorkerComponent workerData = villagerComponent.getWorkerData();
    if (workerData == null) {
      return;
    }

    WorkplaceComponent workerWorkplace = workerData.getWorkplace();
    if (workerWorkplace == null || workerWorkplace.getUuid() == null) {
      return;
    }

    WorkplaceComponent authoritativeWorkplace = villageHeart.getWorkplaceFromId(workerWorkplace.getUuid());
    if (authoritativeWorkplace == null) {
      return;
    }

    workerData.setWorkplace(cloneWorkplace ? authoritativeWorkplace.clone() : authoritativeWorkplace);
  }

  private static boolean isRequestedBy(VillageStorageRequest request, UUID requesterWorkplaceId) {
    if (request == null || requesterWorkplaceId == null) {
      return false;
    }

    return requesterWorkplaceId.equals(request.getRequesterWorkplaceId());
  }

  private static World resolveWorld(Ref<ChunkStore> ref) {
    if (ref == null || !ref.isValid()) {
      return null;
    }

    Store<ChunkStore> chunkStore = ref.getStore();
    BlockModule.BlockStateInfo blockStateInfo = chunkStore.getComponent(
        ref,
        BlockModule.BlockStateInfo.getComponentType());
    if (blockStateInfo == null || blockStateInfo.getChunkRef() == null) {
      return null;
    }

    WorldChunk worldChunk = chunkStore.getComponent(
        blockStateInfo.getChunkRef(),
        WorldChunk.getComponentType());
    return worldChunk != null ? worldChunk.getWorld() : null;
  }

  private static void markChunkComponentDirty(Store<ChunkStore> chunkStore, Ref<ChunkStore> ref) {
    if (chunkStore == null || ref == null || !ref.isValid()) {
      return;
    }

    BlockModule.BlockStateInfo blockStateInfo = chunkStore.getComponent(
        ref,
        BlockModule.BlockStateInfo.getComponentType());
    if (blockStateInfo != null) {
      blockStateInfo.markNeedsSaving();
    }
  }
}
