package com.pacificbytestudios.projectciviccore.village.components.workplace;

import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.container.EmptyItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

// WARN: This won't work in update 4
public class WorkplaceStorageContainerComponent extends ItemContainerState {
  public static final short CONTAINER_CAPACITY = 18;

  public static final BuilderCodec<WorkplaceStorageContainerComponent> CODEC = BuilderCodec
      .builder(WorkplaceStorageContainerComponent.class, WorkplaceStorageContainerComponent::new)

      .append(new KeyedCodec<>("ContainerId", Codec.UUID_BINARY),
          (obj, value) -> obj.uuid = value,
          (obj) -> obj.uuid)
      .add()

      .append(new KeyedCodec<>("Position", Vector3i.CODEC),
          (obj, value) -> obj.position = value,
          (obj) -> obj.position)
      .add()

      .append(new KeyedCodec<>("Storage", ItemContainer.CODEC),
          (obj, value) -> obj.storage = value,
          (obj) -> obj.getItemContainer())
      .add()

      .build();

  private UUID uuid;
  private Vector3i position;
  private ItemContainer storage;

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public Vector3i getPosition() {
    return position;
  }

  public void setPosition(Vector3i position) {
    this.position = position;
  }

  @Override
  public WorkplaceStorageContainerComponent clone() {
    WorkplaceStorageContainerComponent container = new WorkplaceStorageContainerComponent();
    container.uuid = this.uuid;
    container.position = this.position;
    container.storage = this.storage.clone();
    return container;
  }

  public static ComponentType<ChunkStore, WorkplaceStorageContainerComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplaceStorageContainerComponentType();
  }

  public ItemContainer getItemContainer() {
    if (this.storage == null) {
      return EmptyItemContainer.getNewContainer(CONTAINER_CAPACITY, SimpleItemContainer::new);
    }
    return this.storage;
  }
}
