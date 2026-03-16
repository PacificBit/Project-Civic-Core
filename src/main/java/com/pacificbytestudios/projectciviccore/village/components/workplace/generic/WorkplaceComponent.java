package com.pacificbytestudios.projectciviccore.village.components.workplace.generic;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.villagers.enums.WorkType;

public class WorkplaceComponent implements Component<ChunkStore> {
  public static final BuilderCodec<WorkplaceComponent> CODEC = BuilderCodec
      .builder(WorkplaceComponent.class, WorkplaceComponent::new)

      .append(
          new KeyedCodec<>("Uuid", Codec.UUID_BINARY),
          (obj, value) -> obj.uuid = value,
          (obj) -> obj.uuid)
      .add()

      .append(
          new KeyedCodec<>("Type", new EnumCodec<StructureType>(StructureType.class)),
          (obj, value) -> obj.workingPlaceType = value,
          (obj) -> obj.workingPlaceType)
      .add()

      .append(new KeyedCodec<>("BenchPosition", Vector3i.CODEC),
          (obj, value) -> obj.benchPos = value,
          (obj) -> obj.benchPos)
      .add()

      .append(new KeyedCodec<>("PrefabAnchor", Vector3i.CODEC),
          (obj, value) -> obj.prefabAnchor = value,
          (obj) -> obj.prefabAnchor)
      .add()

      .append(new KeyedCodec<>("VillageRef", EntityRef.CODEC),
          (obj, value) -> obj.villageRef = value,
          (obj) -> obj.villageRef)
      .add()

      .append(new KeyedCodec<>("WorkplaceRef", EntityRef.CODEC),
          (obj, value) -> obj.workplaceRef = value,
          (obj) -> obj.workplaceRef)
      .add()

      .append(new KeyedCodec<Set<UUID>>(
          "Workers",
          new SetCodec<UUID, HashSet<UUID>>(Codec.UUID_BINARY, HashSet::new, false)),
          (obj, value) -> obj.workers = value,
          obj -> obj.workers)
      .add()

      .append(new KeyedCodec<>("BossId", Codec.UUID_BINARY),
          (obj, value) -> obj.bossId = value,
          (obj) -> obj.bossId)
      .add()

      .append(new KeyedCodec<>("Storage", WorkplaceContainerRegistryComponent.CODEC),
          (obj, value) -> obj.storage = value,
          (obj) -> obj.storage)
      .add()

      .append(
          new KeyedCodec<>("WorkingArea", ClaimedAreaComponent.CODEC),
          (obj, value) -> obj.workingArea = value,
          (obj) -> obj.workingArea)
      .add()

      .append(
          new KeyedCodec<>("WorkerTypes",
              new ArrayCodec<WorkType>(new EnumCodec<WorkType>(WorkType.class),
                  WorkType[]::new)),
          (obj, value) -> obj.workerType = value,
          (obj) -> obj.workerType)
      .add()

      .append(
          new KeyedCodec<>(
              "WorkersDesignatedArea",
              new MapCodec<ClaimedAreaComponent, HashMap<String, ClaimedAreaComponent>>(
                  ClaimedAreaComponent.CODEC,
                  HashMap::new)),
          (obj, value) -> {
            obj.workersDesignatedArea = new HashMap<>();
            value.forEach((k, v) -> obj.workersDesignatedArea.put(UUID.fromString(k), v));
          },
          obj -> {
            HashMap<String, ClaimedAreaComponent> encoded = new HashMap<>();
            obj.workersDesignatedArea.forEach((k, v) -> encoded.put(k.toString(), v));
            return encoded;
          })
      .add()

      .append(
          new KeyedCodec<>("LastStorageRequest", VillageStorageRequest.CODEC),
          (obj, value) -> obj.lastStorageRequest = value,
          (obj) -> obj.lastStorageRequest)
      .add()

      .build();

  private UUID uuid;
  private StructureType workingPlaceType;
  private WorkType[] workerType;
  private Vector3i benchPos;
  private Vector3i prefabAnchor;

  private EntityRef villageRef;
  private EntityRef workplaceRef;

  private ClaimedAreaComponent workingArea;

  private Set<UUID> workers = new HashSet<>();
  private UUID bossId;
  private Map<UUID, ClaimedAreaComponent> workersDesignatedArea = new HashMap<>();
  private WorkplaceContainerRegistryComponent storage;
  private VillageStorageRequest lastStorageRequest;

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public StructureType getWorkplaceType() {
    return workingPlaceType;
  }

  public void setWorkingPlaceType(StructureType workingPlaceType) {
    this.workingPlaceType = workingPlaceType;
  }

  public Vector3i getBenchPos() {
    return benchPos;
  }

  public void setBenchPos(Vector3i benchPos) {
    this.benchPos = benchPos;
  }

  public Vector3i getPrefabAnchor() {
    return prefabAnchor;
  }

  public void setPrefabAnchor(Vector3i prefabAnchor) {
    this.prefabAnchor = prefabAnchor;
  }

  public void setVillageRef(EntityRef villageRef) {
    this.villageRef = villageRef;
  }

  public Ref<ChunkStore> getVillageHeartRef() {
    return this.villageRef.getRef();
  }

  public EntityRef getWorkplaceRef() {
    return workplaceRef;
  }

  public void setWorkplaceRef(EntityRef workplaceRef) {
    this.workplaceRef = workplaceRef;
  }

  public Set<UUID> getWorkers() {
    return Collections.unmodifiableSet(this.workers);
  }

  public void registerWorker(UUID uuid) {
    this.workers.add(uuid);
    this.recalculateWorkerDesignatedArea();
  }

  public void unregisterWorker(UUID uuid) {
    this.workers.remove(uuid);
    this.workersDesignatedArea.remove(uuid);
    this.recalculateWorkerDesignatedArea();
  }

  public UUID getBossId() {
    return bossId;
  }

  public void registerAsBoss(UUID bossId) {
    this.bossId = bossId;
  }

  private void recalculateWorkerDesignatedArea() {
    if (this.workingArea == null) {
      return;
    }

    ClaimedAreaComponent[] areas = Utils.subdivideArea(this.workingArea, this.workers.size());
    int index = 0;
    for (UUID uuid : this.workers) {
      this.workersDesignatedArea.put(uuid, areas[index++]);
    }
  }

  public void setStorage(WorkplaceContainerRegistryComponent storage) {
    this.storage = storage;
  }

  public WorkplaceContainerRegistryComponent getStorage() {
    return this.storage;
  }

  public ClaimedAreaComponent getWorkingArea() {
    return workingArea;
  }

  public void setWorkingArea(ClaimedAreaComponent claimedArea) {
    this.workingArea = claimedArea;
  }

  public WorkType[] getWorkerType() {
    return workerType;
  }

  public void setWorkerType(WorkType[] workerType) {
    this.workerType = workerType;
  }

  public ClaimedAreaComponent getWorkerDesignatedArea(UUID uuid) {
    return this.workersDesignatedArea.get(uuid);
  }

  public VillageStorageRequest getLastStorageRequest() {
    return lastStorageRequest;
  }

  public void setLastStorageRequest(VillageStorageRequest lastStorageRequest) {
    this.lastStorageRequest = lastStorageRequest;
  }

  @Override
  public WorkplaceComponent clone() {
    WorkplaceComponent component = new WorkplaceComponent();
    component.uuid = this.uuid;
    component.workingPlaceType = this.workingPlaceType;
    component.benchPos = this.benchPos;
    component.villageRef = this.villageRef;
    component.workplaceRef = this.workplaceRef;
    component.workers = new HashSet<>(this.workers);
    component.bossId = this.bossId;
    component.storage = this.storage != null ? this.storage.clone() : null;
    component.prefabAnchor = this.prefabAnchor;
    component.workingArea = this.workingArea != null ? this.workingArea.clone() : null;
    component.workerType = this.workerType != null ? this.workerType.clone() : null;
    component.workersDesignatedArea = new HashMap<>(this.workersDesignatedArea);
    component.lastStorageRequest = this.lastStorageRequest != null ? this.lastStorageRequest.clone() : null;
    return component;
  }

  @Override
  public String toString() {
    return "WorkplaceComponent{" +
        "\n uuid=" + uuid + "," +
        "\n workingPlaceType=" + workingPlaceType + "," +
        "\n benchPos=" + benchPos + "," +
        "\n prefabAnchor=" + prefabAnchor + "," +
        "\n villageHeartLocation=" + villageRef + "," +
        "\n workspaceLocation=" + workplaceRef + "," +
        "\n bossId=" + bossId + "," +
        "\n workers=" + workers + "," +
        "\n typeWorkers=[" + workerType + "]," +
        "\n workersDesignatedArea=" + workersDesignatedArea + "," +
        "\n storage=" + storage + "," +
        "\n lastStorageRequest=" + lastStorageRequest + "," +
        '}';
  }

  public static ComponentType<ChunkStore, WorkplaceComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplaceComponentType();
  }

}
