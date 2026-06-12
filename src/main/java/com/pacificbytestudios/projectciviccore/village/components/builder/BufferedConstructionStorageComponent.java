package com.pacificbytestudios.projectciviccore.village.components.builder;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

@SerializableClass
public class BufferedConstructionStorageComponent implements Component<ChunkStore> {
  public static final BuilderCodec<BufferedConstructionStorageComponent> CODEC = BufferedConstructionStorageComponentCodec.CODEC;

  SimpleItemContainer itemContainer;

  public SimpleItemContainer getItemContainer() {
    return itemContainer;
  }

  public void setItemContainer(SimpleItemContainer itemContainer) {
    this.itemContainer = itemContainer;
  }

  @Override
  public BufferedConstructionStorageComponent clone() {
    return BufferedConstructionStorageComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, BufferedConstructionStorageComponent> getComponentType() {
    return ProjectCivicCore.get().getBufferedConstructionStorageComponentType();
  }
}
