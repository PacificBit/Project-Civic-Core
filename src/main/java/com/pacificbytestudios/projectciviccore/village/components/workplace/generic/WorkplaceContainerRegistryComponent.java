package com.pacificbytestudios.projectciviccore.village.components.workplace.generic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

@SerializableClass
public class WorkplaceContainerRegistryComponent implements Component<ChunkStore> {

  public static final BuilderCodec<WorkplaceContainerRegistryComponent> CODEC = WorkplaceContainerRegistryComponentCodec.CODEC;

  Map<UUID, WorkplaceContainerEntryComponent> containers = new LinkedHashMap<>();

  public Map<UUID, WorkplaceContainerEntryComponent> getContainers() {
    return Collections.unmodifiableMap(containers);
  }

  public List<Vec3i> getAllContainerPositions() {
    return containers.entrySet()
        .stream()
        .map(entry -> entry.getValue().getPosition())
        .toList();
  }

  public List<Vec3f> getAllContainerAccessPositions() {
    return containers.entrySet()
        .stream()
        .map(entry -> entry.getValue().getAccessPosition())
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

  public WorkplaceContainerEntryComponent getFirstContainer() {
    if (this.containers.size() == 0) {
      return null;
    }
    return this.containers.values().iterator().next();
  }

  @Override
  public WorkplaceContainerRegistryComponent clone() {
    return WorkplaceContainerRegistryComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, WorkplaceContainerRegistryComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplaceStorageComponentType();
  }

  @Override
  public String toString() {
    return "WorkplaceContainerRegistryComponent: {\nContainers: " + containers + "\n}";
  }
}
