package com.pacificbytestudios.projectciviccore.villagers.core.work.scan;

import java.util.ArrayDeque;
import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.WorkplaceVillageStorageRequestComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.BlockTarget;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;

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
            FETCH_ALL, request.getTargetsWorkplaceStorage(),
            DUMP_ALL, workplaceComponent.getStorage().getAllContainersTargets());
        break;

      case DELIVER_TO_WORKPLACE:
        putSnapshotTargets(snapshotInProgress,
            FETCH_ITEM, workplaceComponent.getStorage().getAllContainersTargets(),
            DUMP_ITEM, request.getTargetsWorkplaceStorage());
        break;

      case COLLECT_FROM_WORKPLACE:
        putSnapshotTargets(snapshotInProgress,
            FETCH_ITEM, request.getTargetsWorkplaceStorage(),
            DUMP_ITEM, workplaceComponent.getStorage().getAllContainersTargets());
        break;

      default:
        Logger.logWarning("Invalid request type: " + request.getType());
        return snapshotInProgress;
    }

    return signSnapshotAsCompleted(snapshotInProgress, worldTimeResource);
  }

  private void putSnapshotTargets(
      WorkplaceAreaSnapshot snapshot,
      String fetchKey, Iterable<BlockTarget> fetchTargets,
      String dumpKey, Iterable<BlockTarget> dumpTargets) {
    addTargets(snapshot, fetchKey, fetchTargets);
    addTargets(snapshot, dumpKey, dumpTargets);
  }

  private void addTargets(
      WorkplaceAreaSnapshot snapshot,
      String group,
      Iterable<BlockTarget> targets) {
    for (BlockTarget target : targets) {
      snapshot.addWorkplaceSnapshotTarget(group, target);
    }
  }

  private WorkplaceAreaSnapshot signSnapshotAsCompleted(
      WorkplaceAreaSnapshot snapshot,
      WorldTimeResource worldTimeResource) {
    snapshot.setCompleted(true);
    snapshot.captureNow(worldTimeResource);
    return snapshot;
  }
}
