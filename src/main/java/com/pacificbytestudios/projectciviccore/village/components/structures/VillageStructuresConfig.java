package com.pacificbytestudios.projectciviccore.village.components.structures;

import java.util.Map;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.EnumMapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.StructureLevelConfig;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;

public class VillageStructuresConfig implements Component<ChunkStore> {
  public static final BuilderCodec<VillageStructuresConfig> CODEC = BuilderCodec
      .builder(VillageStructuresConfig.class, VillageStructuresConfig::new)

      .append(new KeyedCodec<>(
          "StructureTypeLevelConfig",
          new EnumMapCodec<>(
              StructureType.class,
              StructureLevelConfig.CODEC)),
          (obj, value) -> obj.structureTypeLevelConfigMap = value,
          (obj) -> obj.structureTypeLevelConfigMap)
      .add()

      .build();

  private Map<StructureType, StructureLevelConfig> structureTypeLevelConfigMap;

  @Override
  public VillageStructuresConfig clone() {
    VillageStructuresConfig config = new VillageStructuresConfig();
    config.structureTypeLevelConfigMap = Utils.copyMap(this.structureTypeLevelConfigMap);
    return config;
  }

  public static ComponentType<ChunkStore, VillageStructuresConfig> getComponetType() {
    return ProjectCivicCore.get().getVillageStructureConfigComponentType();
  }
}
