package com.pacificbytestudios.projectciviccore.villagers.components;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.village.components.workplace.WorkplaceComponent;

public class VillagerComponent implements Component<EntityStore> {
  public static final short STORAGE_CAPACITY = 18;
  public static final short HOTBAR_CAPACITY = 3;

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

      .append(new KeyedCodec<>("Workplace", WorkplaceComponent.CODEC),
          (obj, value) -> obj.workplace = value,
          (obj) -> obj.workplace)
      .add()

      .build();

  private EntityRef villageHeartLocation;
  private Vector3i bedPosition;
  private WorkplaceComponent workplace;
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

  public WorkplaceComponent getWorkplace() {
    return workplace;
  }

  public void setWorkplace(WorkplaceComponent workplace) {
    this.workplace = workplace;
  }

  public Map<UUID, ContainerWindow> getWindows() {
    return this.windows;
  }

  @Override
  public VillagerComponent clone() {
    VillagerComponent component = new VillagerComponent();
    component.villageHeartLocation = this.villageHeartLocation == null ? null : this.villageHeartLocation.clone();
    component.bedPosition = this.bedPosition;
    component.workplace = this.workplace == null ? null : this.workplace.clone();
    return component;
  }

  public static ComponentType<EntityStore, VillagerComponent> getComponentType() {
    return ProjectCivicCore.get().getVillagerComponentType();
  }
}
