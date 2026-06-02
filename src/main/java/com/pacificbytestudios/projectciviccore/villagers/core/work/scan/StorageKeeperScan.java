package com.pacificbytestudios.projectciviccore.villagers.core.work.scan;

import java.util.ArrayDeque;
import java.util.UUID;

import org.joml.Vector3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.WorkplaceVillageStorageRequestComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class StorageKeeperScan implements IWorkAreaScanner {
  public static final String FETCH_ALL = "Fetch_All";
  public static final String DUMP_ALL = "Dump_All";
  public static final String FETCH_ITEM = "Fetch_Item";
  public static final String DUMP_ITEM = "Dump_Item";

  @Override
  public WorkplaceAreaSnapshot execute(
      Store<EntityStore> store,
      Ref<EntityStore> villagerRef,
      UUID uuid,
      VillagerComponent villager) {
    VillagerWorkerComponent worker = villager.getWorkerData();
    if (worker == null) {
      Logger.logWarning("Could not fetch workder data");
      return null;
    }

    WorkplaceAreaSnapshot snapshotInProgress = worker.getWorkareaSnapshot();
    WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());

    if (snapshotInProgress != null && snapshotInProgress.isValid(worldTimeResource)) {
      Logger.logInfo("Already has a valid snapshot");
      return snapshotInProgress;
    }

    snapshotInProgress = new WorkplaceAreaSnapshot();

    WorkplaceComponent workplaceComponent = worker.getWorkplace();
    VillageHeartComponent villageHeart = this.getVillageHeart(villager);
    Ref<ChunkStore> storageRef = workplaceComponent.getWorkplaceRef().getRef();
    if (storageRef == null || !storageRef.isValid()) {
      Logger.logWarning("Could not fetch storage reference");
      return snapshotInProgress;
    }

    Store<ChunkStore> chunkStore = storageRef.getStore();
    WorkplaceVillageStorageRequestComponent storageReqComp = chunkStore.getComponent(storageRef,
        WorkplaceVillageStorageRequestComponent.getComponentType());

    if (storageReqComp == null) {
      Logger.logWarning("Could not fetch storage request component");
      return snapshotInProgress;
    }

    ArrayDeque<VillageStorageRequest> requests = storageReqComp.getRequests();
    ArrayDeque<VillageStorageRequest> assignedRequests = storageReqComp.getAssignedRequest(uuid);
    VillageStorageRequest request = assignedRequests.peekFirst();

    if (request == null) {
      request = requests.peekFirst();
      if (request != null && !storageReqComp.assignRequest(uuid, request.getRequestId())) {
        Logger.logWarning("Could not assign request " + request.getRequestId() + " to worker " + uuid);
        return snapshotInProgress;
      }

      assignedRequests = storageReqComp.getAssignedRequest(uuid);
      request = assignedRequests.peekFirst();
    }

    if (request == null) {
      return snapshotInProgress;
    }

    switch (request.getType()) {
      case CLEAR_WORKPLACE_INVENTORY:
        putSnapshotTargets(snapshotInProgress,
            FETCH_ALL, this.resolveRequestStorageTargets(request, villageHeart),
            DUMP_ALL, workplaceComponent.getStorage().getAllContainerAccessPositions());
        break;

      case DELIVER_TO_WORKPLACE:
        putSnapshotTargets(snapshotInProgress,
            FETCH_ITEM, workplaceComponent.getStorage().getAllContainerAccessPositions(),
            DUMP_ITEM, this.resolveRequestStorageTargets(request, villageHeart));
        break;

      case COLLECT_FROM_WORKPLACE:
        putSnapshotTargets(snapshotInProgress,
            FETCH_ITEM, this.resolveRequestStorageTargets(request, villageHeart),
            DUMP_ITEM, workplaceComponent.getStorage().getAllContainerAccessPositions());
        break;

      default:
        Logger.logWarning("Invalid request type: " + request.getType());
        return snapshotInProgress;
    }

    return signSnapshotAsCompleted(snapshotInProgress, worldTimeResource);
  }

  private void putSnapshotTargets(
      WorkplaceAreaSnapshot snapshot,
      String fetchKey, Iterable<Vector3i> fetchTargets,
      String dumpKey, Iterable<Vector3i> dumpTargets) {

    ArrayDeque<Vector3i> fetchFrom = new ArrayDeque<>();
    ArrayDeque<Vector3i> dumpTo = new ArrayDeque<>();

    for (Vector3i position : fetchTargets) {
      fetchFrom.add(position);
    }

    for (Vector3i position : dumpTargets) {
      dumpTo.add(position);
    }

    snapshot.getBlockTypePosition().put(fetchKey, fetchFrom);
    snapshot.getBlockTypePosition().put(dumpKey, dumpTo);
  }

  private WorkplaceAreaSnapshot signSnapshotAsCompleted(
      WorkplaceAreaSnapshot snapshot,
      WorldTimeResource worldTimeResource) {
    snapshot.setCompleted(true);
    snapshot.captureNow(worldTimeResource);
    return snapshot;
  }

  private VillageHeartComponent getVillageHeart(VillagerComponent villager) {
    if (villager == null) {
      return null;
    }

    Ref<ChunkStore> villageHeartRef = villager.getVillageHeartRef();
    if (villageHeartRef == null || !villageHeartRef.isValid()) {
      return null;
    }

    return villageHeartRef.getStore().getComponent(
        villageHeartRef,
        VillageHeartComponent.getComponentType());
  }

  private Iterable<Vector3i> resolveRequestStorageTargets(
      VillageStorageRequest request,
      VillageHeartComponent villageHeart) {
    if (request == null) {
      return new ArrayDeque<>();
    }

    if (villageHeart == null || request.getRequesterWorkplaceId() == null) {
      return request.getTargetsWorkplaceStorage();
    }

    WorkplaceComponent requesterWorkplace = villageHeart.getWorkplaceFromId(request.getRequesterWorkplaceId());
    if (requesterWorkplace == null || requesterWorkplace.getStorage() == null) {
      return request.getTargetsWorkplaceStorage();
    }

    return requesterWorkplace.getStorage().getAllContainerAccessPositions();
  }

}
