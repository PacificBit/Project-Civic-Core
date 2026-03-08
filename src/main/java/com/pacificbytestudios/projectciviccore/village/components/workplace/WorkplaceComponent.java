package com.pacificbytestudios.projectciviccore.village.components.workplace;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceType;
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
          new KeyedCodec<>("Type", new EnumCodec<WorkplaceType>(WorkplaceType.class)),
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

      .append(new KeyedCodec<>("Storage", WorkplaceStorageComponent.CODEC),
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

      .build();

  private UUID uuid;
  private WorkplaceType workingPlaceType;
  private WorkType[] workerType;
  private Vector3i benchPos;
  private Vector3i prefabAnchor;

  private EntityRef villageRef;
  private EntityRef workplaceRef;

  private Set<UUID> workers = new HashSet<>();
  private WorkplaceStorageComponent storage;
  private ClaimedAreaComponent workingArea;

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public WorkplaceType getWorkplaceType() {
    return workingPlaceType;
  }

  public void setWorkingPlaceType(WorkplaceType workingPlaceType) {
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
  }

  public void unregisterWorker(UUID uuid) {
    this.workers.remove(uuid);
  }

  public void setStorage(WorkplaceStorageComponent storage) {
    this.storage = storage;
  }

  public WorkplaceStorageComponent getStorage() {
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

  @Override
  public WorkplaceComponent clone() {
    WorkplaceComponent component = new WorkplaceComponent();
    component.uuid = this.uuid;
    component.workingPlaceType = this.workingPlaceType;
    component.benchPos = this.benchPos;
    component.villageRef = this.villageRef;
    component.workplaceRef = this.workplaceRef;
    component.workers = new HashSet<>(this.workers);
    component.storage = this.storage != null ? this.storage.clone() : null;
    component.prefabAnchor = this.prefabAnchor;
    component.workingArea = this.workingArea != null ? this.workingArea.clone() : null;
    component.workerType = this.workerType != null ? this.workerType.clone() : null;
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
        "\n workers=" + workers + "," +
        "\n typeWorkers=[" + workerType + "]," +
        "\n storage=" + storage + "," +
        '}';
  }

  public static ComponentType<ChunkStore, WorkplaceComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplaceComponentType();
  }

}
