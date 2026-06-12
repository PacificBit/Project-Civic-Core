package com.pacificbytestudios.projectciviccore.village.components.heart;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan;

@SerializableClass
public class VillageSpawnComponent implements Component<ChunkStore> {
  public static final BuilderCodec<VillageSpawnComponent> CODEC = VillageSpawnComponentCodec.CODEC;

  int initialNpcCount;
  int npcSpawnRange;
  VillageProjectPlan structureToSpawn;

  public int getInitialNpcCount() {
    return initialNpcCount;
  }

  public int getNpcSpawnRange() {
    return npcSpawnRange;
  }

  public VillageProjectPlan getStructureToSpawn() {
    return this.structureToSpawn;
  }

  @Override
  public VillageSpawnComponent clone() {
    return VillageSpawnComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, VillageSpawnComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageSpawnComponentType();
  }
}
