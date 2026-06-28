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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

  public BuildingSnapshotComponent clone() {
    return BuildingSnapshotComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, BuildingSnapshotComponent> getComponentType() {
    return ProjectCivicCore.get().getBuildingSnapshotComponent();
  }

}
