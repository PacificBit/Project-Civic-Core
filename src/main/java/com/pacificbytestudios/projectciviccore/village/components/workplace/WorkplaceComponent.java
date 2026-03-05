package com.pacificbytestudios.projectciviccore.village.components.workplace;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplacePlaceType;
import com.pacificbytestudios.projectciviccore.villagers.codecs.VillageHeartRef;

public class WorkplaceComponent implements Component<ChunkStore> {

  public static final BuilderCodec<WorkplaceComponent> CODEC = BuilderCodec
      .builder(WorkplaceComponent.class, WorkplaceComponent::new)

      .append(new KeyedCodec<>("Type", Codec.STRING),
          (obj, value) -> obj.workingPlaceType = WorkplacePlaceType.valueOf(value.toUpperCase()),
          (obj) -> obj.workingPlaceType == null ? null : obj.workingPlaceType.toString())
      .add()

      .append(new KeyedCodec<>("BenchPosition", Vector3i.CODEC),
          (obj, value) -> obj.benchPos = value,
          (obj) -> obj.benchPos)
      .add()

      .append(new KeyedCodec<>("StoragePosition", Vector3i.CODEC),
          (obj, value) -> obj.storagePos = value,
          (obj) -> obj.storagePos)
      .add()

      .append(new KeyedCodec<>("VillageHeartLocation", VillageHeartRef.CODEC),
          (obj, value) -> obj.setVillageHeartLocation(value),
          (obj) -> obj.villageHeartLocation)
      .add()

      .build();

  private WorkplacePlaceType workingPlaceType;
  private Vector3i benchPos;
  private Vector3i storagePos;
  private VillageHeartRef villageHeartLocation;

  public WorkplacePlaceType getWorkingPlaceType() {
    return workingPlaceType;
  }

  public void setWorkingPlaceType(WorkplacePlaceType workingPlaceType) {
    this.workingPlaceType = workingPlaceType;
  }

  public Vector3i getBenchPos() {
    return benchPos;
  }

  public void setBenchPos(Vector3i benchPos) {
    this.benchPos = benchPos;
  }

  public Vector3i getStoragePos() {
    return storagePos;
  }

  public void setStoragePos(Vector3i storagePos) {
    this.storagePos = storagePos;
  }

  public void setVillageHeartLocation(VillageHeartRef villageHeartLocation) {
    this.villageHeartLocation = villageHeartLocation;
  }

  public Ref<ChunkStore> getVillageHeartRef() {
    return this.villageHeartLocation.getRef();
  }

  @Override
  public WorkplaceComponent clone() {
    WorkplaceComponent component = new WorkplaceComponent();
    component.workingPlaceType = this.workingPlaceType;
    component.benchPos = this.benchPos;
    component.storagePos = this.storagePos;
    component.villageHeartLocation = this.villageHeartLocation;
    return component;
  }

  public static ComponentType<ChunkStore, WorkplaceComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplacePlaceComponentType();
  }

}
