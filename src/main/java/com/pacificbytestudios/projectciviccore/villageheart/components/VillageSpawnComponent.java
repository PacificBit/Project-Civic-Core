package com.pacificbytestudios.projectciviccore.villageheart.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public class VillageSpawnComponent implements Component<ChunkStore> {

  public static final BuilderCodec<VillageSpawnComponent> CODEC = BuilderCodec
      .builder(VillageSpawnComponent.class, VillageSpawnComponent::new)

      .append(
          new KeyedCodec<>("InitialNpcCount", Codec.INTEGER),
          (obj, value) -> obj.initialNpcCount = value,
          (obj) -> obj.initialNpcCount)
      .add()

      .append(
          new KeyedCodec<>("NpcSpawnRange", Codec.INTEGER),
          (obj, value) -> obj.spawnNpcRange = value,
          (obj) -> obj.spawnNpcRange)
      .add()

      .build();

  private int initialNpcCount;
  private int spawnNpcRange;

  public int getInitialNpcCount() {
    return initialNpcCount;
  }

  public int getSpawnNpcRange() {
    return spawnNpcRange;
  }

  @Override
  public VillageSpawnComponent clone() {
    VillageSpawnComponent spawn = new VillageSpawnComponent();
    spawn.initialNpcCount = this.initialNpcCount;
    spawn.spawnNpcRange = this.spawnNpcRange;
    return spawn;
  }

  public static ComponentType<ChunkStore, VillageSpawnComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageSpawnComponentType();
  }
}
