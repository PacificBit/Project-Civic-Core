package com.pacificbytestudios.projectciviccore.village.components.heart;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public class VillageHeartMarkerComponent implements Component<ChunkStore> {
  public static final BuilderCodec<VillageHeartMarkerComponent> CODEC = BuilderCodec
      .builder(VillageHeartMarkerComponent.class, VillageHeartMarkerComponent::new)
      .build();

  @Override
  public VillageHeartMarkerComponent clone() {
    VillageHeartMarkerComponent component = new VillageHeartMarkerComponent();
    return component;
  }

  public static ComponentType<ChunkStore, VillageHeartMarkerComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageHeartMarkerComponentType();
  }
}
