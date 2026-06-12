package com.pacificbytestudios.projectciviccore.village.components.workplace.generic;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

@SerializableClass
public class WorkplaceSpawnComponent implements Component<ChunkStore> {
  public static final BuilderCodec<WorkplaceSpawnComponent> CODEC = WorkplaceSpawnComponentCodec.CODEC;

  @Override
  public WorkplaceSpawnComponent clone() {
    return WorkplaceSpawnComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, WorkplaceSpawnComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplaceSpawnComponentType();
  }

}
