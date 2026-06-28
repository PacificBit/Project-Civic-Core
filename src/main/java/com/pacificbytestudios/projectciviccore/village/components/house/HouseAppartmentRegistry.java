package com.pacificbytestudios.projectciviccore.village.components.house;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import lombok.Getter;

@Getter
@SerializableClass
public class HouseAppartmentRegistry implements Component<ChunkStore> {
  public static final BuilderCodec<HouseAppartmentRegistry> CODEC = HouseAppartmentRegistryCodec.CODEC;

  Map<UUID, AppartmentComponent> appartments = new HashMap<>();

  public void registerAppartment(UUID uuid, AppartmentComponent appartment) {
    this.appartments.put(uuid, appartment);
  }

  public void setAppartments(Map<UUID, AppartmentComponent> appartments) {
    this.appartments = appartments;
  }

  public Vec3i getBedPosition(UUID bed) {
    for (Map.Entry<UUID, AppartmentComponent> appts : this.appartments.entrySet()) {
      Vec3i bedPosition = appts.getValue().getBedPosition(bed);
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

  public Set<UUID> getOccupants() {
    Set<UUID> occupants = new HashSet<>();
    for (Map.Entry<UUID, AppartmentComponent> appts : this.appartments.entrySet()) {
      AppartmentComponent value = appts.getValue();
      if (value == null) {
        continue;
      }

      for (VillageBedComponent bed : value.getBeds().values()) {
        if (!bed.isTaken()) {
          continue;
        }
        occupants.add(bed.getOwner());
      }
    }
    return occupants;
  }

  public short getFreeBedsCount() {
    return (short) this.appartments.values()
        .stream()
        .mapToInt(AppartmentComponent::getFreeBedsCount)
        .sum();
  }

  public short getTotalBedsCount() {
    return (short) this.appartments.values()
        .stream()
        .mapToInt(AppartmentComponent::getTotalBedsCount)
        .sum();
  }

  @Override
  public HouseAppartmentRegistry clone() {
    return HouseAppartmentRegistryCodec.clone(this);
  }

  @Override
  public String toString() {
    return "WorkplaceContainerRegistryComponent{\n" +
        "appartments=" + appartments +
        '}';
  }
}
