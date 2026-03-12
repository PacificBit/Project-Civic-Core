
package com.pacificbytestudios.projectciviccore.villagers.core.work.scan;

import java.util.ArrayDeque;
import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.WorkplaceVillageStorageRequestComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
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
    ProjectCivicCore.LOGGER.atInfo().log(
        "[StorageKeeperScan] Starting new storage keeper scan");

    VillagerWorkerComponent worker = villager.getWorkerData();
    if (worker == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[StorageKeeperScan] Could not fetch workder data");
      return null;
    }

    WorkplaceAreaSnapshot snapshotInProgress = worker.getWorkareaSnapshot();
    WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());

    if (snapshotInProgress != null && snapshotInProgress.isValid(worldTimeResource)) {
      ProjectCivicCore.LOGGER.atInfo().log("[StorageKeeperScan] Already has a valid snapshot");
      return snapshotInProgress;
    }

    if (snapshotInProgress == null || snapshotInProgress.isCompleted()) {
      snapshotInProgress = new WorkplaceAreaSnapshot();
    }

    WorkplaceComponent workplaceComponent = worker.getWorkplace();
    Ref<ChunkStore> storageRef = workplaceComponent.getWorkplaceRef().getRef();
    if (storageRef == null || !storageRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[StorageKeeperScan] Could not fetch storage reference");
      return snapshotInProgress;
    }

    Store<ChunkStore> chunkStore = storageRef.getStore();
    WorkplaceVillageStorageRequestComponent storageReqComp = chunkStore.getComponent(storageRef,
        WorkplaceVillageStorageRequestComponent.getComponentType());

    if (storageReqComp == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[StorageKeeperScan] Could not fetch storage request component");
      return snapshotInProgress;
    }

    ArrayDeque<VillageStorageRequest> requests = storageReqComp.getRequests();
    VillageStorageRequest request = requests.peekFirst();

    if (request == null) {
      ProjectCivicCore.LOGGER.atInfo().log("[StorageKeeperScan] No request pending");
      return snapshotInProgress;
    }

    switch (request.getType()) {
      case CLEAR_WORKPLACE_INVENTORY:
        putSnapshotTargets(snapshotInProgress,
            FETCH_ALL, request.getTargetsWorkplaceStorage(),
            DUMP_ALL, workplaceComponent.getStorage().getAllContainerPositions());
        break;

      case DELIVER_TO_WORKPLACE:
        putSnapshotTargets(snapshotInProgress,
            FETCH_ITEM, workplaceComponent.getStorage().getAllContainerPositions(),
            DUMP_ITEM, request.getTargetsWorkplaceStorage());
        break;

      case COLLECT_FROM_WORKPLACE:
        putSnapshotTargets(snapshotInProgress,
            FETCH_ITEM, request.getTargetsWorkplaceStorage(),
            DUMP_ITEM, workplaceComponent.getStorage().getAllContainerPositions());
        break;

      default:
        ProjectCivicCore.LOGGER.atSevere()
            .log("[StorageKeeperScan] Invalid request type: " + request.getType());
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

  private WorkplaceAreaSnapshot signSnapshotAsCompleted(WorkplaceAreaSnapshot snapshot,
      WorldTimeResource worldTimeResource) {
    snapshot.setCompleted(true);
    snapshot.captureNow(worldTimeResource);
    return snapshot;
  }

}
