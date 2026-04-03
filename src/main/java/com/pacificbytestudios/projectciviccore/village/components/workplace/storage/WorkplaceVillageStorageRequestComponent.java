package com.pacificbytestudios.projectciviccore.village.components.workplace.storage;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.ArrayDequeCodec;

public class WorkplaceVillageStorageRequestComponent implements Component<ChunkStore> {
  public static final BuilderCodec<WorkplaceVillageStorageRequestComponent> CODEC = BuilderCodec
      .builder(WorkplaceVillageStorageRequestComponent.class,
          WorkplaceVillageStorageRequestComponent::new)

      .append(new KeyedCodec<>(
          "RequestDeque",
          new ArrayDequeCodec<VillageStorageRequest>(VillageStorageRequest.CODEC,
              "Request")),
          (obj, value) -> obj.requestDeque = value,
          (obj) -> obj.requestDeque)
      .add()

      .append(
          new KeyedCodec<>(
              "OnGoingRequests",
              new MapCodec<ArrayDeque<VillageStorageRequest>, HashMap<String, ArrayDeque<VillageStorageRequest>>>(
                  new ArrayDequeCodec<>(VillageStorageRequest.CODEC, "Request"),
                  HashMap::new)),
          (obj, value) -> obj.onGoingRequests = decodeOnGoingRequests(value),
          obj -> encodeOnGoingRequests(obj.onGoingRequests))
      .add()

      .build();

  private ArrayDeque<VillageStorageRequest> requestDeque = new ArrayDeque<>();
  private Map<UUID, ArrayDeque<VillageStorageRequest>> onGoingRequests = new HashMap<>();

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
    WorkplaceVillageStorageRequestComponent storage = new WorkplaceVillageStorageRequestComponent();
    storage.requestDeque = new ArrayDeque<>();

    for (VillageStorageRequest request : this.requestDeque) {
      storage.requestDeque.add(request != null ? request.clone() : null);
    }

    storage.onGoingRequests = deepCopyOnGoingRequests(this.onGoingRequests);
    return storage;
  }

  public static ComponentType<ChunkStore, WorkplaceVillageStorageRequestComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplaceVillageStorageComponentType();
  }

  private static Map<UUID, ArrayDeque<VillageStorageRequest>> decodeOnGoingRequests(
      Map<String, ArrayDeque<VillageStorageRequest>> value) {
    Map<UUID, ArrayDeque<VillageStorageRequest>> decoded = new HashMap<>();

    if (value == null) {
      return decoded;
    }

    value.forEach((k, v) -> decoded.put(UUID.fromString(k), v));
    return decoded;
  }

  private static HashMap<String, ArrayDeque<VillageStorageRequest>> encodeOnGoingRequests(
      Map<UUID, ArrayDeque<VillageStorageRequest>> value) {
    HashMap<String, ArrayDeque<VillageStorageRequest>> encoded = new HashMap<>();

    if (value == null) {
      return encoded;
    }

    value.forEach((k, v) -> encoded.put(k.toString(), v));
    return encoded;
  }

  private static Map<UUID, ArrayDeque<VillageStorageRequest>> deepCopyOnGoingRequests(
      Map<UUID, ArrayDeque<VillageStorageRequest>> source) {
    Map<UUID, ArrayDeque<VillageStorageRequest>> copy = new HashMap<>();

    if (source == null) {
      return copy;
    }

    source.forEach((k, v) -> copy.put(k, deepCopyRequestDeque(v)));
    return copy;
  }

  private static ArrayDeque<VillageStorageRequest> deepCopyRequestDeque(ArrayDeque<VillageStorageRequest> source) {
    ArrayDeque<VillageStorageRequest> copy = new ArrayDeque<>();

    if (source == null) {
      return copy;
    }

    for (VillageStorageRequest request : source) {
      copy.add(request != null ? request.clone() : null);
    }

    return copy;
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
