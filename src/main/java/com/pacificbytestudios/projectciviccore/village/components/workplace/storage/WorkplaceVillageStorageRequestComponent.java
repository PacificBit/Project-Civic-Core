package com.pacificbytestudios.projectciviccore.village.components.workplace.storage;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

@SerializableClass
public class WorkplaceVillageStorageRequestComponent implements Component<ChunkStore> {
  public static final BuilderCodec<WorkplaceVillageStorageRequestComponent> CODEC = WorkplaceVillageStorageRequestComponentCodec.CODEC;
  ArrayDeque<VillageStorageRequest> requestDeque = new ArrayDeque<>();
  Map<UUID, ArrayDeque<VillageStorageRequest>> onGoingRequests = new HashMap<>();

  public static final class PurgeResult {
    private final Set<UUID> requesterWorkplaceIds = new HashSet<>();
    private final Set<UUID> assignedWorkerIds = new HashSet<>();
    private boolean removedAny;

    public boolean hasRemovedRequests() {
      return this.removedAny;
    }

    public Set<UUID> getRequesterWorkplaceIds() {
      return Collections.unmodifiableSet(this.requesterWorkplaceIds);
    }

    public Set<UUID> getAssignedWorkerIds() {
      return Collections.unmodifiableSet(this.assignedWorkerIds);
    }

    private void registerRemoval(UUID requesterWorkplaceId, UUID assignedWorkerId) {
      this.removedAny = true;

      if (requesterWorkplaceId != null) {
        this.requesterWorkplaceIds.add(requesterWorkplaceId);
      }

      if (assignedWorkerId != null) {
        this.assignedWorkerIds.add(assignedWorkerId);
      }
    }
  }

  public UUID addRequest(VillageStorageRequest workplaceRequest) {
    for (VillageStorageRequest request : this.requestDeque) {
      if (request.getRequesterWorkplaceId().equals(workplaceRequest.getRequesterWorkplaceId()) &&
          request.getType().equals(workplaceRequest.getType())) {
        request.getItemCriteria().addAll(workplaceRequest.getItemCriteria());
        return request.getRequestId();
      }
    }
    this.requestDeque.add(workplaceRequest);
    return workplaceRequest.getRequestId();
  }

  public ArrayDeque<VillageStorageRequest> getRequests() {
    return this.requestDeque;
  }

  public Map<UUID, ArrayDeque<VillageStorageRequest>> getOnGoingRequest() {
    return this.onGoingRequests;
  }

  public boolean assignRequest(UUID workerId, UUID requestId) {
    VillageStorageRequest firstRequest = this.requestDeque.peek();

    if (firstRequest != null && firstRequest.getRequestId().equals(requestId)) {
      this.onGoingRequests.computeIfAbsent(workerId, id -> new ArrayDeque<>())
          .add(this.requestDeque.pollFirst());
      return true;
    }

    ArrayDeque<VillageStorageRequest> remainingRequests = new ArrayDeque<>();
    boolean assigned = false;

    while (!this.requestDeque.isEmpty()) {
      VillageStorageRequest request = this.requestDeque.pollFirst();

      if (!assigned && request.getRequestId().equals(requestId)) {
        this.onGoingRequests.computeIfAbsent(workerId, id -> new ArrayDeque<>())
            .add(request);
        assigned = true;
        continue;
      }

      remainingRequests.add(request);
    }

    this.requestDeque = remainingRequests;
    return assigned;
  }

  public ArrayDeque<VillageStorageRequest> getAssignedRequest(UUID workerId) {
    return this.onGoingRequests.getOrDefault(workerId, new ArrayDeque<>());
  }

  public boolean removeCompletedRequest(UUID workerId) {
    if (this.onGoingRequests.containsKey(workerId)) {
      this.onGoingRequests.remove(workerId);
    }

    return true;
  }

  public boolean unassignRequest(UUID workerId) {
    ArrayDeque<VillageStorageRequest> requests = this.onGoingRequests.remove(workerId);

    if (requests == null || requests.isEmpty()) {
      return false;
    }

    this.requestDeque.addAll(requests);
    return true;
  }

  public PurgeResult purgeRequests(Predicate<VillageStorageRequest> predicate) {
    PurgeResult result = new PurgeResult();
    if (predicate == null) {
      return result;
    }

    this.requestDeque = purgeRequestDeque(this.requestDeque, null, predicate, result);

    Map<UUID, ArrayDeque<VillageStorageRequest>> remainingOnGoingRequests = new HashMap<>();
    for (Map.Entry<UUID, ArrayDeque<VillageStorageRequest>> entry : this.onGoingRequests.entrySet()) {
      ArrayDeque<VillageStorageRequest> remainingRequests = purgeRequestDeque(
          entry.getValue(),
          entry.getKey(),
          predicate,
          result);

      if (!remainingRequests.isEmpty()) {
        remainingOnGoingRequests.put(entry.getKey(), remainingRequests);
      }
    }

    this.onGoingRequests = remainingOnGoingRequests;
    return result;
  }

  public void clearAll() {
    this.requestDeque = new ArrayDeque<>();
    this.onGoingRequests = new HashMap<>();
  }

  @Override
  public WorkplaceVillageStorageRequestComponent clone() {
    return WorkplaceVillageStorageRequestComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, WorkplaceVillageStorageRequestComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplaceVillageStorageComponentType();
  }

  private static ArrayDeque<VillageStorageRequest> purgeRequestDeque(
      ArrayDeque<VillageStorageRequest> source,
      UUID assignedWorkerId,
      Predicate<VillageStorageRequest> predicate,
      PurgeResult result) {
    ArrayDeque<VillageStorageRequest> remainingRequests = new ArrayDeque<>();
    if (source == null) {
      return remainingRequests;
    }

    while (!source.isEmpty()) {
      VillageStorageRequest request = source.pollFirst();

      if (request != null && predicate.test(request)) {
        result.registerRemoval(request.getRequesterWorkplaceId(), assignedWorkerId);
        continue;
      }

      remainingRequests.addLast(request);
    }

    return remainingRequests;
  }
}
