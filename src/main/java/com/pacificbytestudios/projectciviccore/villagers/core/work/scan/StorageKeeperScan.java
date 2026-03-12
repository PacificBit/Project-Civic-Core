
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
import com.pacificbytestudios.projectciviccore.village.enums.VillageStorageRequestType;
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
    VillageStorageRequest request = requests.pollFirst();

    if (request == null) {
      ProjectCivicCore.LOGGER.atInfo().log("[StorageKeeperScan] No request pending");
      return snapshotInProgress;
    }

    // Ref<ChunkStore> villageHeartRef = villager.getVillageHeartRef();
    // if (villageHeartRef == null || !villageHeartRef.isValid()) {
    // ProjectCivicCore.LOGGER.atSevere().log("[StorageKeeperScan] Villager has
    // invalid village heart ref");
    // return snapshotInProgress;
    // }
    //
    // VillageHeartComponent villageHeart = chunkStore.getComponent(villageHeartRef,
    // VillageHeartComponent.getComponentType());
    // if (villageHeart == null) {
    // ProjectCivicCore.LOGGER.atInfo().log("[StorageKeeperScan] Could not fetch the
    // village heart component");
    // return snapshotInProgress;
    // }
    ArrayDeque<Vector3i> fetchFrom = new ArrayDeque<>();
    ArrayDeque<Vector3i> dumpTo = new ArrayDeque<>();

    if (request.getType().equals(VillageStorageRequestType.CLEAR_WORKPLACE_INVENTORY)) {
      fetchFrom.addAll(request.getTargetsWorkplaceStorage());
      dumpTo.addAll(workplaceComponent.getStorage().getAllContainerPositions());
      snapshotInProgress.getBlockTypePosition().put(FETCH_ALL, fetchFrom);
      snapshotInProgress.getBlockTypePosition().put(DUMP_ALL, dumpTo);
      System.out.println("!!!!!!!!!!!!!! Completed snapshot");
      System.out.println(snapshotInProgress);
      storageReqComp.assignRequest(uuid, request.getRequestId());
      return signAsComplete(snapshotInProgress, worldTimeResource);
    }

    // NPCEntity npcEntity = store.getComponent(villagerRef,
    // NPCEntity.getComponentType());
    // int freeSlots =
    // InventoryHelper.countFreeSlots(npcEntity.getInventory().getStorage());

    // boolean keepSearching = true;
    // do {
    // requests.peek().getItemCriteria().size();
    // } while (keepSearching);

    return snapshotInProgress;
  }

  private WorkplaceAreaSnapshot signAsComplete(WorkplaceAreaSnapshot snapshot, WorldTimeResource worldTimeResource) {
    snapshot.setCompleted(true);
    snapshot.captureNow(worldTimeResource);
    return snapshot;
  }

}
