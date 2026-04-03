package com.pacificbytestudios.projectciviccore.villagers.components;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerWindow;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.AreaFootprint;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.villagers.enums.VillagerMealType;
import com.pacificbytestudios.projectciviccore.villagers.enums.VillagerRole;

public class VillagerComponent implements Component<EntityStore> {
  public static final short STORAGE_CAPACITY = 18;
  public static final short HOTBAR_CAPACITY = 1;
  public static final short OFFHAND_CAPACITY = 1;

  public static final BuilderCodec<VillagerComponent> CODEC = BuilderCodec
      .builder(VillagerComponent.class, VillagerComponent::new)

      .append(new KeyedCodec<>("VillageHeartLocation", EntityRef.CODEC),
          (obj, value) -> obj.setVillageHeartLocation(value),
          (obj) -> obj.villageHeartLocation)
      .add()

      .append(new KeyedCodec<>("BedPosition", Vector3i.CODEC),
          (obj, value) -> obj.setBedPosition(value),
          (obj) -> obj.getBedPosition())
      .add()

      .append(new KeyedCodec<>("HousingData",
          VillagerHousingComponent.CODEC),
          (obj, value) -> obj.housingData = value,
          (obj) -> obj.housingData)
      .add()

      .append(new KeyedCodec<>("WorkerData",
          VillagerWorkerComponent.CODEC),
          (obj, value) -> obj.workerData = value,
          (obj) -> obj.workerData)
      .add()

      .append(new KeyedCodec<>("ReservedInteractable", Vector3i.CODEC),
          (obj, value) -> obj.reservedInteractable = value,
          (obj) -> obj.reservedInteractable)
      .add()

      .append(new KeyedCodec<>("ReservedTavernLocation", EntityRef.CODEC, false),
          (obj, value) -> obj.reservedTavernLocation = value,
          (obj) -> obj.reservedTavernLocation)
      .add()

      .append(new KeyedCodec<Instant>("LastLunchMealTime", Codec.INSTANT),
          (obj, value) -> obj.lastLunchMealTime = value,
          (obj) -> obj.lastLunchMealTime)
      .add()

      .append(new KeyedCodec<Instant>("LastDinnerMealTime", Codec.INSTANT),
          (obj, value) -> obj.lastDinnerMealTime = value,
          (obj) -> obj.lastDinnerMealTime)
      .add()

      .append(new KeyedCodec<>("Role", new EnumCodec<>(VillagerRole.class)),
          (obj, value) -> obj.role = value,
          (obj) -> obj.role)
      .add()

      .build();

  private EntityRef villageHeartLocation;
  private VillagerHousingComponent housingData;
  private VillagerWorkerComponent workerData;
  private Vector3i reservedInteractable;
  private EntityRef reservedTavernLocation;
  private Instant lastLunchMealTime;
  private Instant lastDinnerMealTime;
  private VillagerRole role;
  private final Map<UUID, ContainerWindow> windows = new ConcurrentHashMap<>();

  public Vector3i getBedPosition() {
    return this.housingData != null ? this.housingData.getBedPosition() : null;
  }

  public void setBedPosition(Vector3i bedPosition) {
    if (this.housingData == null && bedPosition == null) {
      return;
    }

    this.ensureHousingData().setBedPosition(bedPosition);
  }

  public void setVillageHeartLocation(EntityRef location) {
    this.villageHeartLocation = location;
  }

  public Ref<ChunkStore> getVillageHeartRef() {
    return this.villageHeartLocation.getRef();
  }

  public VillagerWorkerComponent getWorkerData() {
    return workerData;
  }

  public VillagerHousingComponent getHousingData() {
    return this.housingData;
  }

  public void setHousingData(VillagerHousingComponent housingData) {
    this.housingData = housingData;
  }

  public HouseComponent getHouse() {
    return this.housingData != null ? this.housingData.getHouse() : null;
  }

  public void setHouse(HouseComponent house) {
    this.ensureHousingData().setHouse(house);
  }

  public void setWorkerData(VillagerWorkerComponent workerData) {
    this.workerData = workerData;
  }

  public Map<UUID, ContainerWindow> getWindows() {
    return this.windows;
  }

  public Instant getLastMealTime(VillagerMealType mealType) {
    return switch (mealType) {
      case LUNCH -> this.lastLunchMealTime;
      case DINNER -> this.lastDinnerMealTime;
    };
  }

  public void setLastMealTime(VillagerMealType mealType, Instant mealTime) {
    switch (mealType) {
      case LUNCH -> this.lastLunchMealTime = mealTime;
      case DINNER -> this.lastDinnerMealTime = mealTime;
    }
  }

  public void markMealAsConsumed(VillagerMealType mealType, Instant mealTime) {
    this.setLastMealTime(mealType, mealTime);
  }

  public boolean hasConsumedMealOnDate(VillagerMealType mealType, LocalDate currentDate) {
    if (currentDate == null) {
      return false;
    }

    Instant lastMealTime = this.getLastMealTime(mealType);
    if (lastMealTime == null) {
      return false;
    }

    LocalDateTime lastMealDateTime = LocalDateTime.ofInstant(lastMealTime, WorldTimeResource.ZONE_OFFSET);
    return currentDate.equals(lastMealDateTime.toLocalDate());
  }

  public Vector3i getReservedInteractable() {
    return reservedInteractable;
  }

  public void setReservedInteractable(Vector3i reservedInteractable) {
    this.reservedInteractable = reservedInteractable;
  }

  public EntityRef getReservedTavernLocation() {
    return this.reservedTavernLocation;
  }

  public Ref<ChunkStore> getReservedTavernRef() {
    return this.reservedTavernLocation == null ? null : this.reservedTavernLocation.getRef();
  }

  public void setReservedTavernLocation(EntityRef reservedTavernLocation) {
    this.reservedTavernLocation = reservedTavernLocation;
  }

  public void clearReservedTavernReservation() {
    this.reservedInteractable = null;
    this.reservedTavernLocation = null;
  }

  public AreaFootprint getWorkerArea(UUID uuid) {
    return this.getWorkerData().getWorkplace().getWorkerDesignatedArea(uuid);
  }

  public VillagerRole getRole() {
    return role;
  }

  public void setRole(VillagerRole role) {
    this.role = role;
  }

  @Override
  public VillagerComponent clone() {
    VillagerComponent component = new VillagerComponent();
    component.villageHeartLocation = this.villageHeartLocation == null ? null : this.villageHeartLocation.clone();
    component.housingData = this.housingData == null ? null : this.housingData.clone();
    component.workerData = this.workerData == null ? null : this.workerData.clone();
    component.reservedInteractable = this.reservedInteractable == null ? null : this.reservedInteractable.clone();
    component.reservedTavernLocation = this.reservedTavernLocation == null ? null : this.reservedTavernLocation.clone();
    component.lastLunchMealTime = this.lastLunchMealTime;
    component.lastDinnerMealTime = this.lastDinnerMealTime;
    component.role = this.role;
    return component;
  }

  @Override
  public String toString() {
    return "VillagerComponent{" +
        "\n villageHeartLocation=" + Utils.safeToString(villageHeartLocation) +
        ",\n housingData=" + Utils.safeToString(housingData) +
        ",\n workerData=" + Utils.safeToString(workerData) +
        ",\n reservedInteractable=" + Utils.safeToString(reservedInteractable) +
        ",\n reservedTavernLocation=" + Utils.safeToString(reservedTavernLocation) +
        ",\n lastLunchMealTime=" + this.lastLunchMealTime +
        ",\n lastDinnerMealTime=" + this.lastDinnerMealTime +
        ",\n windowsCount=" + this.windows.size() +
        ",\n role=" + this.role +
        '}';
  }

  private VillagerHousingComponent ensureHousingData() {
    if (this.housingData == null) {
      this.housingData = new VillagerHousingComponent();
    }

    return this.housingData;
  }

  public static ComponentType<EntityStore, VillagerComponent> getComponentType() {
    return ProjectCivicCore.get().getVillagerComponentType();
  }
}
