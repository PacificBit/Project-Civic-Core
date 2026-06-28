package com.pacificbytestudios.projectciviccore.villagers.components;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerWindow;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudio.codecbutler.annotations.SerialIgnore;
import com.pacificbytestudio.codecbutler.annotations.SerialWithCodec;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.AreaFootprint;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.villagers.enums.VillagerMealType;
import com.pacificbytestudios.projectciviccore.villagers.enums.VillagerRole;

import lombok.Getter;
import lombok.Setter;

@SerializableClass
public class VillagerComponent implements Component<EntityStore> {
  public static final short STORAGE_CAPACITY = 18;
  public static final short HOTBAR_CAPACITY = 1;
  public static final short OFFHAND_CAPACITY = 1;

  public static final BuilderCodec<VillagerComponent> CODEC = VillagerComponentCodec.CODEC;

  @Setter
  EntityRef villageHeartLocation;
  @Getter
  @Setter
  VillagerHousingComponent housingData;
  @Getter
  @Setter
  VillagerWorkerComponent workerData;
  @Getter
  @Setter
  Vec3f reservedInteractable;
  @Getter
  @Setter
  EntityRef reservedTavernLocation;
  @SerialWithCodec("com.hypixel.hytale.codec.Codec.INSTANT")
  Instant lastLunchMealTime;
  @SerialWithCodec("com.hypixel.hytale.codec.Codec.INSTANT")
  Instant lastDinnerMealTime;
  @Getter
  @Setter
  VillagerRole role;
  @Getter
  @SerialIgnore
  private final Map<UUID, ContainerWindow> windows = new ConcurrentHashMap<>();

  public Vec3i getBedPosition() {
    return this.housingData != null ? this.housingData.getBedPosition() : null;
  }

  public void setBedPosition(Vec3i bedPosition) {
    if (this.housingData == null && bedPosition == null) {
      return;
    }

    this.ensureHousingData().setBedPosition(bedPosition);
  }

  public Ref<ChunkStore> getVillageHeartRef() {
    return this.villageHeartLocation.getRef();
  }

  public HouseComponent getHouse() {
    return this.housingData != null ? this.housingData.getHouse() : null;
  }

  public void setHouse(HouseComponent house) {
    this.ensureHousingData().setHouse(house);
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

  public Ref<ChunkStore> getReservedTavernRef() {
    return this.reservedTavernLocation == null ? null : this.reservedTavernLocation.getRef();
  }

  public void clearReservedTavernReservation() {
    this.reservedInteractable = null;
    this.reservedTavernLocation = null;
  }

  public AreaFootprint getWorkerArea(UUID uuid) {
    return this.getWorkerData().getWorkplace().getWorkerDesignatedArea(uuid);
  }

  @Override
  public VillagerComponent clone() {
    return VillagerComponentCodec.clone(this);
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
