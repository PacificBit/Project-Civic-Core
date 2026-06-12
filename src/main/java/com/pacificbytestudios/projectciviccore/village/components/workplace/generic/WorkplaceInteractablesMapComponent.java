package com.pacificbytestudios.projectciviccore.village.components.workplace.generic;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.Interactable;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.village.enums.InteractablesType;

@SerializableClass
public class WorkplaceInteractablesMapComponent implements Component<ChunkStore> {
  public static final BuilderCodec<WorkplaceInteractablesMapComponent> CODEC = WorkplaceInteractablesMapComponentCodec.CODEC;

  @SerialName("Interactables")
  Map<InteractablesType, Interactable[]> interactable = new HashMap<>();

  public void setNewInteractables(InteractablesType type, List<Interactable> interactables) {
    this.interactable.putIfAbsent(type, new Interactable[0]);
    this.interactable.put(type, interactables.toArray(new Interactable[0]));
  }

  public Interactable getFirstAvailableInteractable(InteractablesType type) {
    Interactable[] interactableGroup = this.interactable.get(type);
    if (interactableGroup == null) {
      throw new IllegalArgumentException("Interactable type '" + type + "' is not defined");
    }

    for (Interactable interactable : interactableGroup) {
      if (interactable.isAvailable()) {
        return interactable;
      }
    }
    return null;
  }

  public Interactable getFirstNotReadyInteractable(InteractablesType type, UUID villager) {
    Interactable[] interactableGroup = this.interactable.get(type);
    if (interactableGroup == null) {
      throw new IllegalArgumentException("Interactable type '" + type + "' is not defined");
    }

    for (Interactable interactable : interactableGroup) {
      if (!interactable.isReady() && (!interactable.isOccupied() || interactable.getOccupiedBy().equals(villager))) {
        return interactable;
      }
    }
    return null;
  }

  public Interactable getInteractableOccupiedBy(InteractablesType type, UUID occupantUuid) {
    Interactable[] interactableGroup = this.interactable.get(type);
    if (interactableGroup == null) {
      throw new IllegalArgumentException("Interactable type '" + type + "' is not defined");
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

  public Interactable getInteractableDependingOn(InteractablesType type, Vec3f dependencyPosition) {
    Interactable[] interactableGroup = this.interactable.get(type);
    if (interactableGroup == null) {
      throw new IllegalArgumentException("Interactable type '" + type + "' is not defined");
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

  public Interactable getInteractableAtPosition(InteractablesType type, Vec3f interactablePosition) {
    Interactable[] interactableGroup = this.interactable.get(type);
    if (interactableGroup == null) {
      throw new IllegalArgumentException("Interactable type '" + type + "' is not defined");
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

  public Interactable[] getAllInteractablesOfGroup(InteractablesType type) {
    return this.interactable.get(type);
  }

  public Map<InteractablesType, Interactable[]> getAllInteractables() {
    return Collections.unmodifiableMap(this.interactable);
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
    return WorkplaceInteractablesMapComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, WorkplaceInteractablesMapComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplaceInteractablesMapComponentType();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("WorkplaceInteractablesMapComponent{\n");

    for (Map.Entry<InteractablesType, Interactable[]> entry : interactable.entrySet()) {
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
