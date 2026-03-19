package com.pacificbytestudios.projectciviccore.village.components.house;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class HouseAppartmentRegistry implements Component<ChunkStore> {

  public static final BuilderCodec<HouseAppartmentRegistry> CODEC = BuilderCodec
      .builder(HouseAppartmentRegistry.class, HouseAppartmentRegistry::new)

      .append(
          new KeyedCodec<>(
              "Appartments",
              new MapCodec<AppartmentComponent, HashMap<String, AppartmentComponent>>(
                  AppartmentComponent.CODEC,
                  HashMap::new)),
          (obj, value) -> {
            obj.appartments = new HashMap<>();
            value.forEach((k, v) -> obj.appartments.put(UUID.fromString(k), v));
          },
          (obj) -> {
            HashMap<String, AppartmentComponent> encoded = new HashMap<>();
            obj.appartments.forEach((k, v) -> encoded.put(k.toString(), v));
            return encoded;
          })
      .add()

      .build();

  private Map<UUID, AppartmentComponent> appartments = new HashMap<>();

  public Map<UUID, AppartmentComponent> getAppartments() {
    return appartments;
  }

  public void registerAppartment(UUID uuid, AppartmentComponent appartment) {
    this.appartments.put(uuid, appartment);
  }

  public void setAppartments(Map<UUID, AppartmentComponent> appartments) {
    this.appartments = appartments;
  }

  public Vector3i getBedPosition(UUID bed) {
    for (Map.Entry<UUID, AppartmentComponent> appts : this.appartments.entrySet()) {
      Vector3i bedPosition = appts.getValue().getBedPosition(bed);
      if (bedPosition != null) {
        return bedPosition;
      }
    }
    return null;
  }

  public UUID assignAppartment(UUID villager) {
    for (Map.Entry<UUID, AppartmentComponent> appts : this.appartments.entrySet()) {
      AppartmentComponent value = appts.getValue();
      if (value == null || value.getFreeBedsCount() == 0) {
        continue;
      }

      UUID assignedBed = value.assignBed(villager);
      if (assignedBed != null) {
        return assignedBed;
      }
    }
    return null;
  }

  public boolean hasVacancy() {
    for (Map.Entry<UUID, AppartmentComponent> appts : this.appartments.entrySet()) {
      if (appts.getValue().getFreeBedsCount() > 0) {
        return true;
      }
    }
    return false;

  }

  public short getFreeBedsCount() {
    return (short) this.appartments.values().stream().mapToInt(AppartmentComponent::getFreeBedsCount).sum();
  }

  @Override
  public HouseAppartmentRegistry clone() {
    HouseAppartmentRegistry component = new HouseAppartmentRegistry();
    component.appartments = new HashMap<>();

    for (Map.Entry<UUID, AppartmentComponent> entry : this.appartments.entrySet()) {
      AppartmentComponent clonedContainer = entry.getValue() != null ? entry.getValue().clone() : null;
      component.appartments.put(entry.getKey(), clonedContainer);
    }

    return component;
  }

  @Override
  public String toString() {
    return "WorkplaceContainerRegistryComponent{\n" +
        "appartments=" + appartments +
        '}';
  }
}
