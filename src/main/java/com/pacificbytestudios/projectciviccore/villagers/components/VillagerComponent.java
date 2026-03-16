package com.pacificbytestudios.projectciviccore.villagers.components;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerWindow;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.villagers.enums.VillagerMealType;

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
          (obj, value) -> obj.bedPosition = value,
          (obj) -> obj.bedPosition)
      .add()

      .append(new KeyedCodec<>("WorkerData",
          VillagerWorkerComponent.CODEC),
          (obj, value) -> obj.workerData = value,
          (obj) -> obj.workerData)
      .add()

      .append(new KeyedCodec<Long>("LastLunchMealDay", Codec.LONG),
          (obj, value) -> obj.lastLunchMealDay = value,
          (obj) -> obj.lastLunchMealDay)
      .add()

      .append(new KeyedCodec<Long>("LastDinnerMealDay", Codec.LONG),
          (obj, value) -> obj.lastDinnerMealDay = value,
          (obj) -> obj.lastDinnerMealDay)
      .add()

      .build();

  private EntityRef villageHeartLocation;
  private Vector3i bedPosition;
  private VillagerWorkerComponent workerData;
  private long lastLunchMealDay = -1L;
  private long lastDinnerMealDay = -1L;

  private final Map<UUID, ContainerWindow> windows = new ConcurrentHashMap<>();

  public Vector3i getBedPosition() {
    return bedPosition;
  }

  public void setBedPosition(Vector3i bedPosition) {
    this.bedPosition = bedPosition;
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

  public void setWorkerData(VillagerWorkerComponent workerData) {
    this.workerData = workerData;
  }

  public Map<UUID, ContainerWindow> getWindows() {
    return this.windows;
  }

  public long getLastMealDay(VillagerMealType mealType) {
    return switch (mealType) {
      case LUNCH -> this.lastLunchMealDay;
      case DINNER -> this.lastDinnerMealDay;
    };
  }

  public void setLastMealDay(VillagerMealType mealType, long mealDay) {
    switch (mealType) {
      case LUNCH -> this.lastLunchMealDay = mealDay;
      case DINNER -> this.lastDinnerMealDay = mealDay;
    }
  }

  public void markMealAsConsumed(VillagerMealType mealType, long mealDay) {
    this.setLastMealDay(mealType, mealDay);
  }

  public boolean hasConsumedMealOnDay(VillagerMealType mealType, long mealDay) {
    return mealDay >= 0L && this.getLastMealDay(mealType) == mealDay;
  }

  public static long buildMealDayKey(LocalDateTime gameDateTime) {
    if (gameDateTime == null) {
      return -1L;
    }

    return (long) gameDateTime.getYear() * 1000L + gameDateTime.getDayOfYear();
  }

  public ClaimedAreaComponent getWorkerArea(UUID uuid) {
    return this.getWorkerData().getWorkplace().getWorkerDesignatedArea(uuid);
  }

  @Override
  public VillagerComponent clone() {
    VillagerComponent component = new VillagerComponent();
    component.villageHeartLocation = this.villageHeartLocation == null ? null : this.villageHeartLocation.clone();
    component.bedPosition = this.bedPosition;
    component.workerData = this.workerData == null ? null : this.workerData.clone();
    return component;
  }

  @Override
  public String toString() {
    return "VillagerComponent{" +
        "villageHeartLocation=" + Utils.safeToString(villageHeartLocation) +
        ", bedPosition=" + Utils.safeToString(bedPosition) +
        ", workerData=" + Utils.safeToString(workerData) +
        ", windowsCount=" + windows.size() +
        '}';
  }

  public static ComponentType<EntityStore, VillagerComponent> getComponentType() {
    return ProjectCivicCore.get().getVillagerComponentType();
  }
}
