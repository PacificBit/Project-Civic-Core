package com.pacificbytestudios.projectciviccore.village.components.builder;

import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;

@SerializableClass
public class BuildingSnapshotComponent implements Component<ChunkStore> {
  public static final BuilderCodec<BuildingSnapshotComponent> CODEC = BuildingSnapshotComponentCodec.CODEC;

  UUID structureId;
  Vec3i prefabAnchor;
  Rotation rotation;
  StructureType structureType;
  EntityRef villageRef;
  short targetLevel;
  short currentStage;

  public UUID getStructureId() {
    return structureId;
  }

  public void setStructureId(UUID structureId) {
    this.structureId = structureId;
  }

  public Vec3i getPrefabAnchor() {
    return prefabAnchor;
  }

  public void setPrefabAnchor(Vec3i prefabAnchor) {
    this.prefabAnchor = prefabAnchor;
  }

  public Rotation getRotation() {
    return rotation;
  }

  public void setRotation(Rotation rotation) {
    this.rotation = rotation;
  }

  public StructureType getStructureType() {
    return structureType;
  }

  public void setStructureType(StructureType structureType) {
    this.structureType = structureType;
  }

  public EntityRef getVillageRef() {
    return villageRef;
  }

  public void setVillageRef(EntityRef villageRef) {
    this.villageRef = villageRef;
  }

  public short getTargetLevel() {
    return targetLevel;
  }

  public void setTargetLevel(short targetLevel) {
    this.targetLevel = targetLevel;
  }

  public short getCurrentStage() {
    return currentStage;
  }

  public void setCurrentStage(short currentStage) {
    this.currentStage = currentStage;
  }

  public BuildingSnapshotComponent clone() {
    return BuildingSnapshotComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, BuildingSnapshotComponent> getComponentType() {
    return ProjectCivicCore.get().getBuildingSnapshotComponent();
  }

}
