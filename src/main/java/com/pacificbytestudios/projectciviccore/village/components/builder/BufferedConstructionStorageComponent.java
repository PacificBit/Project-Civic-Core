package com.pacificbytestudios.projectciviccore.village.components.builder;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public class BufferedConstructionStorageComponent implements Component<ChunkStore> {
  public static final BuilderCodec<BufferedConstructionStorageComponent> CODEC = BuilderCodec
      .builder(BufferedConstructionStorageComponent.class, BufferedConstructionStorageComponent::new)

      .append(new KeyedCodec<>("ItemContainer", SimpleItemContainer.CODEC),
          (obj, value) -> obj.itemContainer = value,
          BufferedConstructionStorageComponent::getItemContainer)
      .add()

      .build();

  private SimpleItemContainer itemContainer;

  public SimpleItemContainer getItemContainer() {
    return itemContainer;
  }

  public void setItemContainer(SimpleItemContainer itemContainer) {
    this.itemContainer = itemContainer;
  }

  @Override
  public BufferedConstructionStorageComponent clone() {
    BufferedConstructionStorageComponent component = new BufferedConstructionStorageComponent();
    component.itemContainer = this.itemContainer == null ? null : this.itemContainer.clone();
    return component;
  }

  public static ComponentType<ChunkStore, BufferedConstructionStorageComponent> getComponentType() {
    return ProjectCivicCore.get().getBufferedConstructionStorageComponentType();
  }
}
