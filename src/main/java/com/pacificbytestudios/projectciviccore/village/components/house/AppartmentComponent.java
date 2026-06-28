package com.pacificbytestudios.projectciviccore.village.components.house;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import lombok.Getter;

@Getter
@SerializableClass
public class AppartmentComponent implements Component<ChunkStore> {
  public static final BuilderCodec<AppartmentComponent> CODEC = AppartmentComponentCodec.CODEC;

  Map<UUID, VillageBedComponent> beds = new HashMap<>();

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

  public Vec3i getBedPosition(UUID bed) {
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
    return AppartmentComponentCodec.clone(this);
  }
}
