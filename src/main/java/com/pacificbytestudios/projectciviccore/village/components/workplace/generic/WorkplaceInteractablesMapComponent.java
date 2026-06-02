package com.pacificbytestudios.projectciviccore.village.components.workplace.generic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.EnumMapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.Interactable;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceInteractibles;

public class WorkplaceInteractablesMapComponent implements Component<ChunkStore> {
  public static final BuilderCodec<WorkplaceInteractablesMapComponent> CODEC = BuilderCodec
      .builder(WorkplaceInteractablesMapComponent.class, WorkplaceInteractablesMapComponent::new)

      .append(
          new KeyedCodec<>("Interactables", new EnumMapCodec<>(
              WorkplaceInteractibles.class,
              new ArrayCodec<>(
                  Interactable.CODEC,
                  Interactable[]::new))),
          (obj, value) -> obj.interactable = value,
          obj -> obj.interactable)
      .add()

      .build();

  private Map<WorkplaceInteractibles, Interactable[]> interactable = new HashMap<>();

  public void setNewInteractables(WorkplaceInteractibles group, List<Interactable> interactables) {
    this.interactable.putIfAbsent(group, new Interactable[0]);
    this.interactable.put(group, interactables.toArray(new Interactable[0]));
  }

  public Interactable getFirstAvailableInteractable(WorkplaceInteractibles group) {
    Interactable[] interactableGroup = this.interactable.get(group);
    if (interactableGroup == null) {
      throw new IllegalArgumentException("Interactable group '" + group + "' is not defined");
    }

    for (Interactable interactable : interactableGroup) {
      if (interactable.isAvailable()) {
        return interactable;
      }
    }
    return null;
  }

  public Interactable getFirstNotReadyInteractable(WorkplaceInteractibles group, UUID villager) {
    Interactable[] interactableGroup = this.interactable.get(group);
    if (interactableGroup == null) {
      throw new IllegalArgumentException("Interactable group '" + group + "' is not defined");
    }

    for (Interactable interactable : interactableGroup) {
      if (!interactable.isReady() && (!interactable.isOccupied() || interactable.getOccupiedBy().equals(villager))) {
        return interactable;
      }
    }
    return null;
  }

  public Interactable getInteractableOccupiedBy(WorkplaceInteractibles group, UUID occupantUuid) {
    Interactable[] interactableGroup = this.interactable.get(group);
    if (interactableGroup == null) {
      throw new IllegalArgumentException("Interactable group '" + group + "' is not defined");
    }

    if (occupantUuid == null) {
      return null;
    }

    for (Interactable interactable : interactableGroup) {
      if (interactable != null
          && interactable.isOccupied()
          && occupantUuid.equals(interactable.getOccupiedBy())) {
        return interactable;
      }
    }

    return null;
  }

  public Interactable getInteractableDependingOn(WorkplaceInteractibles group, Vec3i dependencyPosition) {
    Interactable[] interactableGroup = this.interactable.get(group);
    if (interactableGroup == null) {
      throw new IllegalArgumentException("Interactable group '" + group + "' is not defined");
    }

    if (dependencyPosition == null) {
      return null;
    }

    for (Interactable interactable : interactableGroup) {
      if (interactable != null && interactable.dependsOn(dependencyPosition)) {
        return interactable;
      }
    }

    return null;
  }

  public Interactable getInteractableAtPosition(WorkplaceInteractibles group, Vec3i interactablePosition) {
    Interactable[] interactableGroup = this.interactable.get(group);
    if (interactableGroup == null) {
      throw new IllegalArgumentException("Interactable group '" + group + "' is not defined");
    }

    if (interactablePosition == null) {
      return null;
    }

    for (Interactable interactable : interactableGroup) {
      if (interactable != null && interactablePosition.equals(interactable.getInteractablePosition())) {
        return interactable;
      }
    }

    return null;
  }

  public boolean releaseInteractablesOccupiedBy(UUID occupantUuid) {
    if (occupantUuid == null) {
      return false;
    }

    boolean released = false;
    for (Interactable[] interactableGroup : this.interactable.values()) {
      if (interactableGroup == null) {
        continue;
      }

      for (Interactable interactable : interactableGroup) {
        if (interactable == null || !occupantUuid.equals(interactable.getOccupiedBy())) {
          continue;
        }

        if (interactable.isInvalidateOnUse()) {
          interactable.setReady(false);
        }
        interactable.setOccupiedBy(null);
        released = true;
      }
    }

    return released;
  }

  @Override
  public WorkplaceInteractablesMapComponent clone() {
    WorkplaceInteractablesMapComponent component = new WorkplaceInteractablesMapComponent();
    component.interactable = deepCopy(this.interactable);
    return component;
  }

  private static Map<WorkplaceInteractibles, Interactable[]> deepCopy(
      Map<WorkplaceInteractibles, Interactable[]> source) {
    Map<WorkplaceInteractibles, Interactable[]> copy = new HashMap<>();

    if (source == null) {
      return copy;
    }

    for (Map.Entry<WorkplaceInteractibles, Interactable[]> entry : source.entrySet()) {
      WorkplaceInteractibles key = entry.getKey();
      Interactable[] value = entry.getValue();

      if (value == null) {
        copy.put(key, null);
        continue;
      }

      Interactable[] clonedArray = new Interactable[value.length];
      for (int i = 0; i < value.length; i++) {
        Interactable interactable = value[i];
        clonedArray[i] = interactable != null ? interactable.clone() : null;
      }

      copy.put(key, clonedArray);
    }

    return copy;
  }

  public static ComponentType<ChunkStore, WorkplaceInteractablesMapComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplaceInteractablesMapComponentType();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("WorkplaceInteractablesMapComponent{\n");

    for (Map.Entry<WorkplaceInteractibles, Interactable[]> entry : interactable.entrySet()) {
      sb.append("  ").append(entry.getKey()).append("={\n");

      Interactable[] values = entry.getValue();
      if (values != null) {
        for (Interactable value : values) {
          sb.append("    - ").append(value).append("\n");
        }
      }
      sb.append("  }\n");
    }
    sb.append("}");
    return sb.toString();
  }
}
