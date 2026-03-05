package com.pacificbytestudios.projectciviccore.village.components.workplace;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public class WorkplaceSpawnComponent implements Component<ChunkStore> {
  public static final BuilderCodec<WorkplaceSpawnComponent> CODEC = BuilderCodec
      .builder(WorkplaceSpawnComponent.class, WorkplaceSpawnComponent::new)
      .build();

  @Override
  public WorkplaceSpawnComponent clone() {
    WorkplaceSpawnComponent component = new WorkplaceSpawnComponent();
    return component;
  }

  public static ComponentType<ChunkStore, WorkplaceSpawnComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplacePlaceSpawnComponentType();
  }

}
