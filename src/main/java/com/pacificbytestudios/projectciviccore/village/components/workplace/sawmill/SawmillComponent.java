package com.pacificbytestudios.projectciviccore.village.components.workplace.sawmill;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest.ItemRequestCriterion;
import com.pacificbytestudios.projectciviccore.village.core.interfaces.IWorkplaceMissingResource;

@SerializableClass
public class SawmillComponent implements Component<ChunkStore>, IWorkplaceMissingResource {
  public static final BuilderCodec<SawmillComponent> CODEC = SawmillComponentCodec.CODEC;

  Set<ItemRequestCriterion> missingResources = new HashSet<>();

  @Override
  public Set<ItemRequestCriterion> getMissingResources() {
    return missingResources;
  }

  @Override
  public void newMissingResouceBuffer() {
    this.missingResources = new HashSet<>();
  }

  @Override
  public void setMissingResource(List<ItemRequestCriterion> criterion) {
    this.missingResources.addAll(criterion);
  }

  @Override
  public void addMissingResource(ItemRequestCriterion criterion) {
    this.missingResources.add(criterion);
  }

  @Override
  public void resetMissingResources() {
    this.newMissingResouceBuffer();
  }

  @Override
  public SawmillComponent clone() {
    return SawmillComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, SawmillComponent> getComponentType() {
    return ProjectCivicCore.get().getSawmillComponentType();
  }

}
