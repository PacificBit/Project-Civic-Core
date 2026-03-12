package com.pacificbytestudios.projectciviccore.village.components.workplace.generic;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public class WorkplaceContainerRegistryComponent implements Component<ChunkStore> {

  public static final BuilderCodec<WorkplaceContainerRegistryComponent> CODEC = BuilderCodec
      .builder(WorkplaceContainerRegistryComponent.class, WorkplaceContainerRegistryComponent::new)

      .append(
          new KeyedCodec<>(
              "Containers",
              new MapCodec<WorkplaceContainerEntryComponent, HashMap<String, WorkplaceContainerEntryComponent>>(
                  WorkplaceContainerEntryComponent.CODEC,
                  HashMap::new)),
          (obj, value) -> {
            obj.containers = new HashMap<>();
            value.forEach((k, v) -> obj.containers.put(UUID.fromString(k), v));
          },
          (obj) -> {
            HashMap<String, WorkplaceContainerEntryComponent> encoded = new HashMap<>();
            obj.containers.forEach((k, v) -> encoded.put(k.toString(), v));
            return encoded;
          })
      .add()

      .build();

  private Map<UUID, WorkplaceContainerEntryComponent> containers = new HashMap<>();

  public Map<UUID, WorkplaceContainerEntryComponent> getContainers() {
    return Collections.unmodifiableMap(containers);
  }

  public List<Vector3i> getAllContainerPositions() {
    return containers.entrySet()
        .stream()
        .map(entry -> entry.getValue().getPosition())
        .toList();
  }

  public void registerContainer(UUID uuid, WorkplaceContainerEntryComponent container) {
    this.containers.put(uuid, container);
  }

  public void unregisterContainer(UUID uuid) {
    if (!this.containers.containsKey(uuid)) {
      return;
    }
    this.containers.remove(uuid);
  }

  @Override
  public WorkplaceContainerRegistryComponent clone() {
    WorkplaceContainerRegistryComponent component = new WorkplaceContainerRegistryComponent();
    component.containers = new HashMap<>();

    for (Map.Entry<UUID, WorkplaceContainerEntryComponent> entry : this.containers.entrySet()) {
      WorkplaceContainerEntryComponent clonedContainer = entry.getValue() != null ? entry.getValue().clone() : null;
      component.containers.put(entry.getKey(), clonedContainer);
    }

    return component;
  }

  public static ComponentType<ChunkStore, WorkplaceContainerRegistryComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplaceStorageComponentType();
  }

}
