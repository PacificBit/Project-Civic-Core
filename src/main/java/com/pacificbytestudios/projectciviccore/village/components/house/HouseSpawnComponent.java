package com.pacificbytestudios.projectciviccore.village.components.house;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public class HouseSpawnComponent implements Component<ChunkStore> {
  public static final BuilderCodec<HouseSpawnComponent> CODEC = BuilderCodec
      .builder(HouseSpawnComponent.class, HouseSpawnComponent::new)
      .build();

  @Override
  public HouseSpawnComponent clone() {
    HouseSpawnComponent component = new HouseSpawnComponent();
    return component;
  }

  public static ComponentType<ChunkStore, HouseSpawnComponent> getComponentType() {
    return ProjectCivicCore.get().getHouseSpawnComponentType();
  }

}
