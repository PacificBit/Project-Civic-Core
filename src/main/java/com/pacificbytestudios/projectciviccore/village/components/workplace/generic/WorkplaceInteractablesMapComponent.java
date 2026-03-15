package com.pacificbytestudios.projectciviccore.village.components.workplace.generic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.Vector3iMapCodec;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceInteractibles;

public class WorkplaceInteractablesMapComponent implements
    Component<ChunkStore> {
  private static final SetCodec<UUID, HashSet<UUID>> OCCUPANT_SET_CODEC = new SetCodec<>(
      Codec.UUID_STRING,
      HashSet::new,
      false);

  private static final Vector3iMapCodec<Set<UUID>, HashMap<Vector3i, Set<UUID>>> POSITION_OCCUPIED_CODEC = new Vector3iMapCodec<>(
      OCCUPANT_SET_CODEC,
      HashMap::new,
      false);

  public static final BuilderCodec<WorkplaceInteractablesMapComponent> CODEC = BuilderCodec
      .builder(WorkplaceInteractablesMapComponent.class,
          WorkplaceInteractablesMapComponent::new)

      .append(new KeyedCodec<>(
          "Interactable",
          new MapCodec<>(POSITION_OCCUPIED_CODEC, HashMap::new, false)),
          (obj, value) -> obj.interactable = deepCopy(value),
          obj -> deepCopy(obj.interactable))
      .add()

      .build();

  private Map<String, Map<Vector3i, Set<UUID>>> interactable = new HashMap<>();

  public void setNewInteractable(String group, Vector3i position) {
    this.interactable.putIfAbsent(group, new HashMap<>());
    this.interactable.get(group).putIfAbsent(new Vector3i(position), new HashSet<>());
  }

  public void setNewInteractable(WorkplaceInteractibles group, List<Vector3i> positions) {
    setNewInteractable(group.getKey(), positions);
  }

  public void setNewInteractable(String group, List<Vector3i> positions) {
    positions.forEach(pos -> setNewInteractable(group, pos));
  }

  public boolean isOccupied(String group, Vector3i position) {
    return !this.getOccupants(group, position).isEmpty();
  }

  public boolean isOccupiedBy(String group, Vector3i position, UUID occupierId) {
    return this.getOccupants(group, position).contains(occupierId);
  }

  public Vector3i getOccupiedPosition(String group, UUID occupierId) {
    Map<Vector3i, Set<UUID>> interactableGroup = this.interactable.get(group);
    if (interactableGroup == null) {
      throw new IllegalArgumentException("Interactable group '" + group + "' is not defined");
    }

    for (Entry<Vector3i, Set<UUID>> entries : interactableGroup.entrySet()) {
      if (entries.getValue().contains(occupierId)) {
        return entries.getKey();
      }
    }
    return null;
  }

  public int getOccupantCount(String group, Vector3i position) {
    return this.getOccupants(group, position).size();
  }

  public Set<UUID> getOccupants(String group, Vector3i position) {
    Map<Vector3i, Set<UUID>> interactableGroup = this.interactable.get(group);
    if (interactableGroup == null) {
      throw new IllegalArgumentException("Interactable group '" + group + "' is not defined");
    }

    Set<UUID> occupants = interactableGroup.get(position);
    if (occupants == null) {
      throw new IllegalArgumentException("Interactable position '" + position + "' is not defined");
    }

    return occupants;
  }

  public void registerAsOccupied(WorkplaceInteractibles group, Vector3i position, UUID occupierId) {
    registerAsOccupied(group.getKey(), position, occupierId);
  }

  public void registerAsOccupied(String group, Vector3i position, UUID occupierId) {
    this.getOccupants(group, position).add(occupierId);
  }

  public void registerAsUnoccupied(String group, Vector3i position, UUID occupierId) {
    this.getOccupants(group, position).remove(occupierId);
  }

  @Nullable
  public Vector3i getFirstInteractablePositionUnoccupied(WorkplaceInteractibles interactibles) {
    return getFirstInteractablePositionUnoccupied(interactibles.getKey());
  }

  @Nullable
  public Vector3i getFirstInteractablePositionUnoccupied(String group) {
    Map<Vector3i, Set<UUID>> interactableGroup = this.interactable.get(group);
    if (interactableGroup == null) {
      throw new IllegalArgumentException("Interactable group '" + group + "' is not defined");
    }

    for (Entry<Vector3i, Set<UUID>> entries : interactableGroup.entrySet()) {
      if (entries.getValue().isEmpty()) {
        return entries.getKey();
      }
    }

    return null;
  }

  public void resetInteractableGroup(String group) {
    Map<Vector3i, Set<UUID>> interactableGroup = this.interactable.get(group);
    if (interactableGroup == null) {
      throw new IllegalArgumentException("Interactable group '" + group + "' is not defined");
    }

    interactableGroup.values().forEach(Set::clear);
  }

  public void resetAll() {
    this.interactable.keySet().forEach(this::resetInteractableGroup);
  }

  @Override
  public WorkplaceInteractablesMapComponent clone() {
    WorkplaceInteractablesMapComponent component = new WorkplaceInteractablesMapComponent();
    component.interactable = deepCopy(this.interactable);
    return component;
  }

  public static ComponentType<ChunkStore, WorkplaceInteractablesMapComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplaceInteractablesMapComponentType();
  }

  private static Map<String, Map<Vector3i, Set<UUID>>> deepCopy(Map<String, Map<Vector3i, Set<UUID>>> source) {
    Map<String, Map<Vector3i, Set<UUID>>> copy = new HashMap<>();

    if (source == null) {
      return copy;
    }

    source.forEach((group, positions) -> {
      Map<Vector3i, Set<UUID>> positionCopy = new HashMap<>();

      if (positions != null) {
        positions.forEach((pos, occupants) -> positionCopy.put(
            pos == null ? null : new Vector3i(pos),
            occupants == null ? new HashSet<>() : new HashSet<>(occupants)));
      }

      copy.put(group, positionCopy);
    });

    return copy;
  }
}
