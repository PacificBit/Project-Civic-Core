package com.pacificbytestudios.projectciviccore.village.components.house;

import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;

public class HouseComponent implements Component<ChunkStore> {
  public static final BuilderCodec<HouseComponent> CODEC = BuilderCodec
      .builder(HouseComponent.class, HouseComponent::new)

      .append(new KeyedCodec<>("HouseId", Codec.UUID_BINARY),
          (obj, value) -> obj.uuid = value,
          (obj) -> obj.uuid)
      .add()

      .append(
          new KeyedCodec<>("Level", Codec.SHORT),
          (obj, value) -> obj.level = value,
          (obj) -> obj.level)
      .add()

      .append(new KeyedCodec<>("PrefabAnchor", Vector3i.CODEC),
          (obj, value) -> obj.prefabAnchor = value,
          (obj) -> obj.prefabAnchor)
      .add()

      .append(new KeyedCodec<>("CoreBlockPosition", Vector3i.CODEC),
          (obj, value) -> obj.coreBlockPosition = value,
          (obj) -> obj.coreBlockPosition)
      .add()

      .append(new KeyedCodec<>("VillageRef", EntityRef.CODEC),
          (obj, value) -> obj.villageRef = value,
          (obj) -> obj.villageRef)
      .add()

      .append(new KeyedCodec<>("HouseRef", EntityRef.CODEC),
          (obj, value) -> obj.houseRef = value,
          (obj) -> obj.houseRef)
      .add()

      .append(new KeyedCodec<>("AppartmentRegistry", HouseAppartmentRegistry.CODEC),
          (obj, value) -> obj.appartmentRegistry = value,
          (obj) -> obj.appartmentRegistry)
      .add()
      .build();

  private UUID uuid;
  private Short level = 1;
  private EntityRef villageRef;
  private Vector3i coreBlockPosition;
  private Vector3i prefabAnchor;
  private EntityRef houseRef;
  private HouseAppartmentRegistry appartmentRegistry;

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

  public EntityRef getVillageRef() {
    return villageRef;
  }

  public void setVillageRef(EntityRef villageRef) {
    this.villageRef = villageRef;
  }

  public Vector3i getCoreBlockPosition() {
    return coreBlockPosition;
  }

  public void setCoreBlockPosition(Vector3i coreBlockPosition) {
    this.coreBlockPosition = coreBlockPosition;
  }

  public Vector3i getPrefabAnchor() {
    return prefabAnchor;
  }

  public void setPrefabAnchor(Vector3i prefabAnchor) {
    this.prefabAnchor = prefabAnchor;
  }

  public EntityRef getHouseRef() {
    return houseRef;
  }

  public void setHouseRef(EntityRef houseRef) {
    this.houseRef = houseRef;
  }

  public HouseAppartmentRegistry getAppartmentRegistry() {
    return appartmentRegistry;
  }

  public void setAppartmentRegistry(HouseAppartmentRegistry appartmentRegistry) {
    this.appartmentRegistry = appartmentRegistry;
  }

  public UUID assignAppartment(UUID villager) {
    return this.appartmentRegistry.assignAppartment(villager);
  }

  public Vector3i getBedPosition(UUID bed) {
    return this.appartmentRegistry.getBedPosition(bed);
  }

  public boolean hasVacancy() {
    return this.appartmentRegistry.hasVacancy();
  }

  public short getFreeBedCount() {
    return this.appartmentRegistry.getFreeBedsCount();
  }

  @Override
  public HouseComponent clone() {
    HouseComponent component = new HouseComponent();
    component.uuid = this.uuid;
    component.prefabAnchor = this.prefabAnchor == null ? null : this.prefabAnchor.clone();
    component.coreBlockPosition = this.coreBlockPosition == null ? null : this.coreBlockPosition.clone();
    component.villageRef = this.villageRef == null ? null : this.villageRef.clone();
    component.houseRef = this.houseRef == null ? null : this.houseRef.clone();
    component.appartmentRegistry = this.appartmentRegistry == null ? null : this.appartmentRegistry.clone();
    return component;
  }

  public static ComponentType<ChunkStore, HouseComponent> getComponentType() {
    return ProjectCivicCore.get().getHouseComponentType();
  }

}
