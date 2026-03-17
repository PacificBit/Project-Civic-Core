package com.pacificbytestudios.projectciviccore.village.components.workplace.farm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest.ItemRequestCriterion;
import com.pacificbytestudios.projectciviccore.village.core.interfaces.IWorkplaceMissingResource;

public class FarmComponent implements Component<ChunkStore>, IWorkplaceMissingResource {
  public static final BuilderCodec<FarmComponent> CODEC = BuilderCodec
      .builder(FarmComponent.class, FarmComponent::new)

      .append(new KeyedCodec<Set<ItemRequestCriterion>>(
          "MissingResources",
          new SetCodec<ItemRequestCriterion, HashSet<ItemRequestCriterion>>(
              ItemRequestCriterion.CODEC,
              HashSet::new,
              false)),
          (obj, value) -> obj.missingResources = value,
          obj -> obj.missingResources)
      .add()

      .build();

  private Set<ItemRequestCriterion> missingResources = new HashSet<>();

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
  public FarmComponent clone() {
    FarmComponent component = new FarmComponent();
    component.missingResources = new HashSet<>();

    for (ItemRequestCriterion criterion : this.missingResources) {
      component.missingResources.add(criterion != null ? criterion.clone() : null);
    }
    return component;
  }

  public static ComponentType<ChunkStore, FarmComponent> getComponentType() {
    return ProjectCivicCore.get().getFarmComponentType();
  }

}
