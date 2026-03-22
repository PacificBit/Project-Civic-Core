package com.pacificbytestudios.projectciviccore.village.components.heart;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan;

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

      .append(new KeyedCodec<>(
          "StructureToSpawn",
          VillageProjectPlan.CODEC),
          (obj, value) -> obj.structureToSpawn = value,
          (obj) -> obj.structureToSpawn)
      .add()

      .build();

  private int initialNpcCount;
  private int spawnNpcRange;
  private VillageProjectPlan structureToSpawn;

  public int getInitialNpcCount() {
    return initialNpcCount;
  }

  public int getSpawnNpcRange() {
    return spawnNpcRange;
  }

  public VillageProjectPlan getStructureToSpawn() {
    return this.structureToSpawn;
  }

  @Override
  public VillageSpawnComponent clone() {
    VillageSpawnComponent spawn = new VillageSpawnComponent();
    spawn.initialNpcCount = this.initialNpcCount;
    spawn.spawnNpcRange = this.spawnNpcRange;
    spawn.structureToSpawn = this.structureToSpawn == null ? null : this.structureToSpawn.clone();
    return spawn;
  }

  public static ComponentType<ChunkStore, VillageSpawnComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageSpawnComponentType();
  }
}
