package com.pacificbytestudios.projectciviccore.villagers.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.codec.VillageHeartRef;

public class VillagerComponent implements Component<EntityStore> {
  public static final BuilderCodec<VillagerComponent> CODEC = BuilderCodec
      .builder(VillagerComponent.class, VillagerComponent::new)

      .append(new KeyedCodec<>("VillageHeartLocation", VillageHeartRef.CODEC),
          (obj, value) -> obj.setVillageHeartLocation(value),
          (obj) -> obj.villageHeartLocation)
      .add()

      .append(new KeyedCodec<>("BedPosition", Vector3i.CODEC),
          (obj, value) -> obj.bedPosition = value,
          (obj) -> obj.bedPosition)
      .add()

      .build();

  private VillageHeartRef villageHeartLocation;
  private Vector3i bedPosition;

  public Vector3i getBedPosition() {
    return bedPosition;
  }

  public void setBedPosition(Vector3i bedPosition) {
    this.bedPosition = bedPosition;
  }

  public void setVillageHeartLocation(VillageHeartRef location) {
    this.villageHeartLocation = location;
  }

  public Ref<ChunkStore> getVillageHeartRef() {
    return this.villageHeartLocation.getRef();
  }

  @Override
  public VillagerComponent clone() {
    VillagerComponent newComponent = new VillagerComponent();
    newComponent.villageHeartLocation = this.villageHeartLocation;
    newComponent.bedPosition = this.bedPosition;
    return newComponent;
  }

  public static ComponentType<EntityStore, VillagerComponent> getComponentType() {
    return ProjectCivicCore.get().getVillagerComponentType();
  }
}
