package com.pacificbytestudios.projectciviccore.village.components.structures;

import java.util.Map;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.StructureLevelConfig;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;

@SerializableClass
public class VillageStructuresConfig implements Component<ChunkStore> {
  public static final BuilderCodec<VillageStructuresConfig> CODEC = VillageStructuresConfigCodec.CODEC;

  @SerialName("StructureTypeLevelConfig")
  Map<StructureType, StructureLevelConfig> structureTypeLevelConfigMap;

  @Override
  public VillageStructuresConfig clone() {
    return VillageStructuresConfigCodec.clone(this);
  }

  public StructureLevelConfig getStructureLevelConfigsFor(StructureType type) {
    return this.structureTypeLevelConfigMap.get(type);
  }

  public static ComponentType<ChunkStore, VillageStructuresConfig> getComponetType() {
    return ProjectCivicCore.get().getVillageStructureConfigComponentType();
  }
}
