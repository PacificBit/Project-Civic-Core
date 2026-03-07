package com.pacificbytestudios.projectciviccore.village.components.workplace;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.codecs.VillageHeartRef;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceType;

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

      .append(new KeyedCodec<>("VillageHeartLocation", VillageHeartRef.CODEC),
          (obj, value) -> obj.setVillageHeartLocation(value),
          (obj) -> obj.villageHeartLocation)
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

      .build();

  private UUID uuid;
  private WorkplaceType workingPlaceType;
  private Vector3i benchPos;
  private Vector3i prefabAnchor;

  private VillageHeartRef villageHeartLocation;
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

  public void setVillageHeartLocation(VillageHeartRef villageHeartLocation) {
    this.villageHeartLocation = villageHeartLocation;
  }

  public Ref<ChunkStore> getVillageHeartRef() {
    return this.villageHeartLocation.getRef();
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

  @Override
  public WorkplaceComponent clone() {
    WorkplaceComponent component = new WorkplaceComponent();
    component.uuid = this.uuid;
    component.workingPlaceType = this.workingPlaceType;
    component.benchPos = this.benchPos;
    component.villageHeartLocation = this.villageHeartLocation;
    component.workers = new HashSet<>(this.workers);
    component.storage = this.storage != null ? this.storage.clone() : null;
    component.prefabAnchor = this.prefabAnchor;
    component.workingArea = this.workingArea != null ? this.workingArea.clone() : null;
    return component;
  }

  @Override
  public String toString() {
    return "WorkplaceComponent{" +
        "\n uuid=" + uuid + "," +
        "\n workingPlaceType=" + workingPlaceType + "," +
        "\n benchPos=" + benchPos + "," +
        "\n prefabAnchor=" + prefabAnchor + "," +
        "\n villageHeartLocation=" + villageHeartLocation + "," +
        "\n workers=" + workers + "," +
        "\n storage=" + storage + "," +
        '}';
  }

  public static ComponentType<ChunkStore, WorkplaceComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplaceComponentType();
  }

}
