package com.pacificbytestudios.projectciviccore.village.components.builder;

import java.util.UUID;

import org.joml.Vector3i;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;

public class BuildingSnapshotComponent implements Component<ChunkStore> {
  public static final BuilderCodec<BuildingSnapshotComponent> CODEC = BuilderCodec
      .builder(BuildingSnapshotComponent.class, BuildingSnapshotComponent::new)

      .append(new KeyedCodec<>("StructureId", Codec.UUID_BINARY),
          (obj, value) -> obj.structureId = value,
          (obj) -> obj.structureId)
      .add()

      .append(new KeyedCodec<>("VillageRef", EntityRef.CODEC),
          (obj, value) -> obj.villageRef = value,
          (obj) -> obj.villageRef)
      .add()

      .append(new KeyedCodec<>("PrefabAnchor", Vector3iUtil.CODEC),
          (obj, value) -> obj.prefabAnchor = value,
          (obj) -> obj.prefabAnchor)
      .add()

      .append(new KeyedCodec<>("Rotation", Rotation.CODEC),
          (obj, value) -> obj.rotation = value,
          (obj) -> obj.rotation)
      .add()

      .append(
          new KeyedCodec<>("StructureType", new EnumCodec<StructureType>(StructureType.class)),
          (obj, value) -> obj.structureType = value,
          (obj) -> obj.structureType)
      .add()

      .append(new KeyedCodec<>("TargetLevel", Codec.SHORT),
          (obj, value) -> obj.targetLevel = value,
          (obj) -> obj.targetLevel)
      .add()

      .append(new KeyedCodec<>("CurrentStage", Codec.SHORT),
          (obj, value) -> obj.currentStage = value,
          (obj) -> obj.currentStage)
      .add()

      .build();

  private UUID structureId;
  private Vector3i prefabAnchor;
  private Rotation rotation;
  private StructureType structureType;
  private EntityRef villageRef;
  private short targetLevel;
  private short currentStage;

  public UUID getStructureId() {
    return structureId;
  }

  public void setStructureId(UUID structureId) {
    this.structureId = structureId;
  }

  public Vector3i getPrefabAnchor() {
    return prefabAnchor;
  }

  public void setPrefabAnchor(Vector3i prefabAnchor) {
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
    BuildingSnapshotComponent component = new BuildingSnapshotComponent();
    component.structureId = this.structureId;
    component.prefabAnchor = VectorUtil.clone(this.prefabAnchor);
    component.rotation = this.rotation;
    component.structureType = this.structureType;
    component.villageRef = this.villageRef == null ? null : this.villageRef.clone();
    component.currentStage = this.currentStage;
    component.targetLevel = this.targetLevel;
    return component;
  }

  public static ComponentType<ChunkStore, BuildingSnapshotComponent> getComponentType() {
    return ProjectCivicCore.get().getBuildingSnapshotComponent();
  }

}
