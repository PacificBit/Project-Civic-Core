package com.pacificbytestudios.projectciviccore.village.components.workplace.generic;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.AreaFootprint;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.villagers.enums.WorkType;

@SerializableClass
public class WorkplaceComponent implements Component<ChunkStore> {
  public static final BuilderCodec<WorkplaceComponent> CODEC = WorkplaceComponentCodec.CODEC;

  @SerialName("Type")
  StructureType workingPlaceType;
  @SerialName("WorkerTypes")
  WorkType[] workerType;

  UUID uuid;
  Short level = 1;
  EntityRef villageRef;
  EntityRef workplaceRef;
  AreaFootprint workingArea;

  BlockTransform<Vec3i> benchTransform;
  BlockTransform<Vec3i> prefabAnchorTransform;

  UUID bossId;
  Set<UUID> workers = new HashSet<>();
  Map<UUID, AreaFootprint> workersDesignatedArea = new HashMap<>();
  WorkplaceContainerRegistryComponent storage;
  VillageStorageRequest lastStorageRequest;

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public Short getLevel() {
    return level;
  }

  public void setLevel(Short level) {
    this.level = level;
  }

  public StructureType getWorkplaceType() {
    return workingPlaceType;
  }

  public void setWorkingPlaceType(StructureType workingPlaceType) {
    this.workingPlaceType = workingPlaceType;
  }

  public Vec3i getBenchPos() {
    return this.benchTransform.getPosition();
  }

  public void setBenchPos(Vec3i benchPos) {
    this.benchTransform = new BlockTransform<>(benchPos);
  }

  public BlockTransform<Vec3i> getBenchTransform() {
    return this.benchTransform;
  }

  public void setBenchTransform(BlockTransform<Vec3i> benchTransform) {
    this.benchTransform = benchTransform;
  }

  public Vec3f getBenchAccessPos() {
    return Vec3f.from(this.benchTransform.getPosition()).add(this.benchTransform.getAccessOffset());
  }

  public void setBenchAccessOffset(Vec3f benchAccessOffset) {
    this.benchTransform.setAccessOffset(Vec3f.from(benchAccessOffset));
  }

  public Vec3i getPrefabAnchor() {
    return this.prefabAnchorTransform.getPosition();
  }

  public void setPrefabAnchor(Vec3i prefabAnchor) {
    this.prefabAnchorTransform = new BlockTransform<>(prefabAnchor);
  }

  public BlockTransform<Vec3i> getPrefabAnchorTransform() {
    return this.prefabAnchorTransform;
  }

  public void setPrefabAnchorTransform(BlockTransform<Vec3i> prefabAnchorTransform) {
    this.prefabAnchorTransform = prefabAnchorTransform;
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

    if (this.workers.size() > 0) {
      this.recalculateWorkerDesignatedArea();
    }
  }

  public boolean needWorker() {
    return this.workers.size() < this.getMaxWorkerCount();
  }

  public int getMaxWorkerCount() {
    int configuredCount = this.workerType == null || this.workerType.length == 0
        ? 1
        : this.workerType.length;

    if (this.workingPlaceType == StructureType.STORAGE && this.level == 2) {
      return Math.max(configuredCount, 4);
    }

    return configuredCount;
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

    AreaFootprint[] areas = Utils.subdivideArea(this.workingArea, this.workers.size());
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

  public AreaFootprint getWorkingArea() {
    return workingArea;
  }

  public void setWorkingArea(AreaFootprint claimedArea) {
    this.workingArea = claimedArea;
  }

  public WorkType[] getWorkerType() {
    return workerType;
  }

  public void setWorkerType(WorkType[] workerType) {
    this.workerType = workerType;
  }

  public AreaFootprint getWorkerDesignatedArea(UUID uuid) {
    return this.workersDesignatedArea.get(uuid);
  }

  public VillageStorageRequest getLastStorageRequest() {
    return lastStorageRequest;
  }

  public void setLastStorageRequest(VillageStorageRequest lastStorageRequest) {
    this.lastStorageRequest = lastStorageRequest;
  }

  public <T extends Component<ChunkStore>> T getComponent(ComponentType<ChunkStore, T> componentType) {
    Ref<ChunkStore> ref = this.workplaceRef.getRef();
    return ref.getStore().getComponent(ref, componentType);
  }

  @Override
  public WorkplaceComponent clone() {
    return WorkplaceComponentCodec.clone(this);
  }

  @Override
  public String toString() {
    return "WorkplaceComponent{" +
        "\n\tuuid=" + uuid + "," +
        "\n\tlevel=" + level + "," +
        "\n\tworkingPlaceType=" + workingPlaceType + "," +
        "\n\tbenchPos=" + getBenchPos() + "," +
        "\n\tbenchAccessPos=" + getBenchAccessPos() + "," +
        "\n\tprefabAnchor=" + getPrefabAnchor() + "," +
        "\n\tvillageHeartLocation=" + villageRef + "," +
        "\n\tworkspaceLocation=" + workplaceRef + "," +
        "\n\tbossId=" + bossId + "," +
        "\n\tworkers=" + workers + "," +
        "\n\ttypeWorkers=[" + workerType + "]," +
        "\n\tworkersDesignatedArea=" + workersDesignatedArea + "," +
        "\n\tstorage=" + storage + "," +
        "\n\tlastStorageRequest=" + lastStorageRequest + "," +
        '}';
  }

  public static ComponentType<ChunkStore, WorkplaceComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplaceComponentType();
  }
}
