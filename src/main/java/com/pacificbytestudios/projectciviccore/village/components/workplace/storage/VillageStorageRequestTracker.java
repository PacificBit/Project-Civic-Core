package com.pacificbytestudios.projectciviccore.village.components.workplace.storage;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.AVillageTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.EnqueueVillageStorageRequestTask;
import com.pacificbytestudios.projectciviccore.village.enums.VillageStorageRequestType;

public final class VillageStorageRequestTracker {
  private VillageStorageRequestTracker() {
  }

  public static boolean hasPendingRequest(
      Ref<ChunkStore> villageHeartRef,
      VillageHeartComponent villageHeart,
      UUID requesterWorkplaceId,
      VillageStorageRequestType requestType) {
    if (hasPendingVillageTask(villageHeartRef, requesterWorkplaceId, requestType)) {
      return true;
    }

    return hasPendingRequest(villageHeart, requesterWorkplaceId, requestType);
  }

  public static boolean hasPendingRequest(
      VillageHeartComponent villageHeart,
      UUID requesterWorkplaceId,
      VillageStorageRequestType requestType) {
    if (villageHeart == null || requesterWorkplaceId == null) {
      return false;
    }

    for (UUID storageWorkplaceId : villageHeart.getVillageStorageWorkplaces()) {
      WorkplaceComponent storageWorkplace = villageHeart.getWorkplaceFromId(storageWorkplaceId);
      if (storageWorkplace == null || storageWorkplace.getWorkplaceRef() == null) {
        continue;
      }

      Ref<ChunkStore> storageRef = storageWorkplace.getWorkplaceRef().getRef();
      if (storageRef == null || !storageRef.isValid()) {
        continue;
      }

      WorkplaceVillageStorageRequestComponent storageRequestComponent = storageRef.getStore().getComponent(
          storageRef,
          WorkplaceVillageStorageRequestComponent.getComponentType());
      if (storageRequestComponent == null) {
        continue;
      }

      if (containsMatchingRequest(storageRequestComponent.getRequests(), requesterWorkplaceId, requestType)) {
        return true;
      }

      for (Map.Entry<UUID, ArrayDeque<VillageStorageRequest>> entry : storageRequestComponent.getOnGoingRequest()
          .entrySet()) {
        if (containsMatchingRequest(entry.getValue(), requesterWorkplaceId, requestType)) {
          return true;
        }
      }
    }

    return false;
  }

  private static boolean hasPendingVillageTask(
      Ref<ChunkStore> villageHeartRef,
      UUID requesterWorkplaceId,
      VillageStorageRequestType requestType) {
    if (villageHeartRef == null || !villageHeartRef.isValid() || requesterWorkplaceId == null) {
      return false;
    }

    VillageTaskQueueComponent taskQueue = villageHeartRef.getStore().getComponent(
        villageHeartRef,
        VillageTaskQueueComponent.getComponentType());
    if (taskQueue == null) {
      return false;
    }

    for (AVillageTask task : taskQueue.getTasks()) {
      if (!(task instanceof EnqueueVillageStorageRequestTask storageTask)) {
        continue;
      }

      if (matches(storageTask.getRequest(), requesterWorkplaceId, requestType)) {
        return true;
      }
    }

    return false;
  }

  private static boolean containsMatchingRequest(
      Iterable<VillageStorageRequest> requests,
      UUID requesterWorkplaceId,
      VillageStorageRequestType requestType) {
    if (requests == null) {
      return false;
    }

    for (VillageStorageRequest request : requests) {
      if (matches(request, requesterWorkplaceId, requestType)) {
        return true;
      }
    }

    return false;
  }

  private static boolean matches(
      VillageStorageRequest request,
      UUID requesterWorkplaceId,
      VillageStorageRequestType requestType) {
    if (request == null || request.getRequesterWorkplaceId() == null) {
      return false;
    }

    if (!request.getRequesterWorkplaceId().equals(requesterWorkplaceId)) {
      return false;
    }

    return requestType == null || requestType.equals(request.getType());
  }
}
// package
// com.pacificbytestudios.projectciviccore.village.components.workplace.storage;
//
// import java.util.ArrayDeque;
// import java.util.Map;
// import java.util.UUID;
//
// import com.hypixel.hytale.component.Ref;
// import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
// import
// com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
// import
// com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
// import
// com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
// import
// com.pacificbytestudios.projectciviccore.village.core.villagetask.AVillageTask;
// import
// com.pacificbytestudios.projectciviccore.village.core.villagetask.EnqueueVillageStorageRequestTask;
// import
// com.pacificbytestudios.projectciviccore.village.enums.VillageStorageRequestType;
//
// public final class VillageStorageRequestTracker {
// private VillageStorageRequestTracker() {
// }
//
// public static boolean hasPendingRequest(
// Ref<ChunkStore> villageHeartRef,
// VillageHeartComponent villageHeart,
// UUID requesterWorkplaceId) {
// return hasPendingRequest(villageHeartRef, villageHeart, requesterWorkplaceId,
// null);
// }
//
// public static boolean hasPendingRequest(
// Ref<ChunkStore> villageHeartRef,
// VillageHeartComponent villageHeart,
// UUID requesterWorkplaceId,
// VillageStorageRequestType requestType) {
// if (hasPendingVillageTask(villageHeartRef, requesterWorkplaceId,
// requestType)) {
// return true;
// }
//
// return hasPendingRequest(villageHeart, requesterWorkplaceId, requestType);
// }
//
// public static boolean hasPendingRequest(
// VillageHeartComponent villageHeart,
// UUID requesterWorkplaceId) {
// return hasPendingRequest(villageHeart, requesterWorkplaceId, null);
// }
//
// public static boolean hasPendingRequest(
// VillageHeartComponent villageHeart,
// UUID requesterWorkplaceId,
// VillageStorageRequestType requestType) {
// if (villageHeart == null || requesterWorkplaceId == null) {
// return false;
// }
//
// for (UUID storageWorkplaceId : villageHeart.getVillageStorageWorkplaces()) {
// WorkplaceComponent storageWorkplace =
// villageHeart.getWorkplaceFromId(storageWorkplaceId);
// if (storageWorkplace == null || storageWorkplace.getWorkplaceRef() == null) {
// continue;
// }
//
// Ref<ChunkStore> storageRef = storageWorkplace.getWorkplaceRef().getRef();
// if (storageRef == null || !storageRef.isValid()) {
// continue;
// }
//
// WorkplaceVillageStorageRequestComponent storageRequestComponent =
// storageRef.getStore().getComponent(
// storageRef,
// WorkplaceVillageStorageRequestComponent.getComponentType());
// if (storageRequestComponent == null) {
// continue;
// }
//
// if (containsMatchingRequest(storageRequestComponent.getRequests(),
// requesterWorkplaceId, requestType)) {
// return true;
// }
//
// for (Map.Entry<UUID, ArrayDeque<VillageStorageRequest>> entry :
// storageRequestComponent.getOnGoingRequest()
// .entrySet()) {
// if (containsMatchingRequest(entry.getValue(), requesterWorkplaceId,
// requestType)) {
// return true;
// }
// }
// }
//
// return false;
// }
//
// private static boolean hasPendingVillageTask(
// Ref<ChunkStore> villageHeartRef,
// UUID requesterWorkplaceId,
// VillageStorageRequestType requestType) {
// if (villageHeartRef == null || !villageHeartRef.isValid() ||
// requesterWorkplaceId == null) {
// return false;
// }
//
// VillageTaskQueueComponent taskQueue =
// villageHeartRef.getStore().getComponent(
// villageHeartRef,
// VillageTaskQueueComponent.getComponentType());
// if (taskQueue == null) {
// return false;
// }
//
// for (AVillageTask task : taskQueue.getTasks()) {
// if (!(task instanceof EnqueueVillageStorageRequestTask storageTask)) {
// continue;
// }
//
// if (matches(storageTask.getRequest(), requesterWorkplaceId, requestType)) {
// return true;
// }
// }
//
// return false;
// }
//
// private static boolean containsMatchingRequest(
// Iterable<VillageStorageRequest> requests,
// UUID requesterWorkplaceId,
// VillageStorageRequestType requestType) {
// if (requests == null) {
// return false;
// }
//
// for (VillageStorageRequest request : requests) {
// if (matches(request, requesterWorkplaceId, requestType)) {
// return true;
// }
// }
//
// return false;
// }
//
// private static boolean matches(
// VillageStorageRequest request,
// UUID requesterWorkplaceId,
// VillageStorageRequestType requestType) {
// if (request == null || request.getRequesterWorkplaceId() == null) {
// return false;
// }
//
// if (!request.getRequesterWorkplaceId().equals(requesterWorkplaceId)) {
// return false;
// }
//
// return requestType == null || requestType.equals(request.getType());
// }
// }
