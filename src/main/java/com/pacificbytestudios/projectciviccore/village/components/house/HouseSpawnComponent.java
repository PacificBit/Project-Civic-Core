package com.pacificbytestudios.projectciviccore.village.components.house;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

@SerializableClass
public class HouseSpawnComponent implements Component<ChunkStore> {
  public static final BuilderCodec<HouseSpawnComponent> CODEC = HouseSpawnComponentCodec.CODEC;

  @Override
  public HouseSpawnComponent clone() {
    return HouseSpawnComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, HouseSpawnComponent> getComponentType() {
    return ProjectCivicCore.get().getHouseSpawnComponentType();
  }

}
