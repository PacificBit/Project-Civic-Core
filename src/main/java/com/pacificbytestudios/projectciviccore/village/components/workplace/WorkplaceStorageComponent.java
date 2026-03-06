package com.pacificbytestudios.projectciviccore.village.components.workplace;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public class WorkplaceStorageComponent implements Component<ChunkStore> {

  public static final BuilderCodec<WorkplaceStorageComponent> CODEC = BuilderCodec
      .builder(WorkplaceStorageComponent.class, WorkplaceStorageComponent::new)

      .append(
          new KeyedCodec<>(
              "Containers",
              new MapCodec<WorkplaceStorageContainerComponent, HashMap<String, WorkplaceStorageContainerComponent>>(
                  WorkplaceStorageContainerComponent.CODEC,
                  HashMap::new)),
          (obj, value) -> {
            obj.containers = new HashMap<>();
            value.forEach((k, v) -> obj.containers.put(UUID.fromString(k), v));
          },
          obj -> {
            HashMap<String, WorkplaceStorageContainerComponent> encoded = new HashMap<>();
            obj.containers.forEach((k, v) -> encoded.put(k.toString(), v));
            return encoded;
          })
      .add()

      .build();

  private Map<UUID, WorkplaceStorageContainerComponent> containers = new HashMap<>();

  public Map<UUID, WorkplaceStorageContainerComponent> getContainers() {
    return Collections.unmodifiableMap(containers);
  }

  public void registerContainer(UUID uuid, WorkplaceStorageContainerComponent container) {
    this.containers.put(uuid, container);
  }

  public void unregisterContainer(UUID uuid) {
    if (!this.containers.containsKey(uuid)) {
      return;
    }
    this.containers.remove(uuid);
  }

  @Override
  public WorkplaceStorageComponent clone() {
    WorkplaceStorageComponent component = new WorkplaceStorageComponent();
    component.containers = this.containers;
    return component;
  }

  public static ComponentType<ChunkStore, WorkplaceStorageComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplaceStorageComponentType();
  }

}
