package com.pacificbytestudios.projectciviccore.village.components.house;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.joml.Vector3i;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class AppartmentComponent implements Component<ChunkStore> {

  public static final BuilderCodec<AppartmentComponent> CODEC = BuilderCodec
      .builder(AppartmentComponent.class, AppartmentComponent::new)

      .append(
          new KeyedCodec<>(
              "Beds",
              new MapCodec<VillageBedComponent, HashMap<String, VillageBedComponent>>(
                  VillageBedComponent.CODEC,
                  HashMap::new)),
          (obj, value) -> {
            obj.beds = new HashMap<>();
            value.forEach((k, v) -> obj.beds.put(UUID.fromString(k), v));
          },
          (obj) -> {
            HashMap<String, VillageBedComponent> encoded = new HashMap<>();
            obj.beds.forEach((k, v) -> encoded.put(k.toString(), v));
            return encoded;
          })
      .add()

      .build();

  private Map<UUID, VillageBedComponent> beds = new HashMap<>();

  public Map<UUID, VillageBedComponent> getBeds() {
    return this.beds;
  }

  public void setBeds(Map<UUID, VillageBedComponent> beds) {
    this.beds = beds;
  }

  public short getTotalBedsCount() {
    return (short) beds.values()
        .stream()
        .filter(bed -> bed != null)
        .count();
  }

  public short getFreeBedsCount() {
    short registeredBeds = getTotalBedsCount();
    short bedsTaken = (short) beds.values()
        .stream()
        .filter(bed -> bed != null)
        .filter(VillageBedComponent::isTaken)
        .count();
    return (short) (registeredBeds - bedsTaken);
  }

  public Vector3i getBedPosition(UUID bed) {
    VillageBedComponent bedComponent = this.beds.get(bed);
    if (bedComponent == null) {
      return null;
    }
    return bedComponent.getPosition();
  }

  public UUID assignBed(UUID villager) {
    for (Map.Entry<UUID, VillageBedComponent> entry : this.beds.entrySet()) {
      VillageBedComponent bedComponent = entry.getValue();
      if (bedComponent == null || bedComponent.isTaken()) {
        continue;
      }

      bedComponent.setOwner(villager);
      bedComponent.setTaken(true);
      return entry.getKey();
    }

    return null;
  }

  @Override
  public AppartmentComponent clone() {
    AppartmentComponent component = new AppartmentComponent();
    component.beds = new HashMap<>();

    for (Map.Entry<UUID, VillageBedComponent> entry : this.beds.entrySet()) {
      VillageBedComponent clonedContainer = entry.getValue() != null ? entry.getValue().clone() : null;
      component.beds.put(entry.getKey(), clonedContainer);
    }
    return component;
  }
}
