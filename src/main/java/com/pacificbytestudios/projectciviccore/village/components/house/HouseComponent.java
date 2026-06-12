package com.pacificbytestudios.projectciviccore.village.components.house;

import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

@SerializableClass
public class HouseComponent implements Component<ChunkStore> {
  public static final BuilderCodec<HouseComponent> CODEC = HouseComponentCodec.CODEC;

  @SerialName("HouseId")
  UUID uuid;
  Short level = 1;
  EntityRef villageRef;
  Vec3i coreBlockPosition;
  Vec3i prefabAnchor;
  EntityRef houseRef;
  HouseAppartmentRegistry appartmentRegistry;

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

  public Vec3i getCoreBlockPosition() {
    return coreBlockPosition;
  }

  public void setCoreBlockPosition(Vec3i coreBlockPosition) {
    this.coreBlockPosition = coreBlockPosition;
  }

  public Vec3i getPrefabAnchor() {
    return prefabAnchor;
  }

  public void setPrefabAnchor(Vec3i prefabAnchor) {
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

  public Set<UUID> getOccupants() {
    return this.getAppartmentRegistry().getOccupants();
  }

  public void setAppartmentRegistry(HouseAppartmentRegistry appartmentRegistry) {
    this.appartmentRegistry = appartmentRegistry;
  }

  public UUID assignAppartment(UUID villager) {
    return this.appartmentRegistry.assignAppartment(villager);
  }

  public Vec3i getBedPosition(UUID bed) {
    return this.appartmentRegistry.getBedPosition(bed);
  }

  public boolean hasVacancy() {
    return this.appartmentRegistry.hasVacancy();
  }

  public short getFreeBedCount() {
    return this.appartmentRegistry.getFreeBedsCount();
  }

  public short getTotalBedsCount() {
    return this.appartmentRegistry.getTotalBedsCount();
  }

  @Override
  public HouseComponent clone() {
    return HouseComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, HouseComponent> getComponentType() {
    return ProjectCivicCore.get().getHouseComponentType();
  }

}
