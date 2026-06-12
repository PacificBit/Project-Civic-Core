package com.pacificbytestudios.projectciviccore.village.components.heart;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

@SerializableClass
public class VillageHeartMarkerComponent implements Component<ChunkStore> {
  public static final BuilderCodec<VillageHeartMarkerComponent> CODEC = VillageHeartMarkerComponentCodec.CODEC;

  @Override
  public VillageHeartMarkerComponent clone() {
    return VillageHeartMarkerComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, VillageHeartMarkerComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageHeartMarkerComponentType();
  }
}
