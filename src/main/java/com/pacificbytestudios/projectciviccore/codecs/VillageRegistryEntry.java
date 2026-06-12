package com.pacificbytestudios.projectciviccore.codecs;

import java.util.Objects;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudio.codecbutler.annotations.SerialName;

@SerializableClass
public class VillageRegistryEntry {
  public static final BuilderCodec<VillageRegistryEntry> CODEC = VillageRegistryEntryCodec.CODEC;

  UUID villageId;
  String villageName;
  @SerialName("VillageContext")
  BlockContext villageCoreWorldContext;
  AreaFootprint claimedArea;

  public UUID getVillageId() {
    return villageId;
  }

  public void setVillageId(UUID villageId) {
    this.villageId = villageId;
  }

  public String getVillageName() {
    return villageName;
  }

  public void setVillageName(String villageName) {
    this.villageName = villageName;
  }

  public BlockContext getVillageCoreWorldContext() {
    return villageCoreWorldContext;
  }

  public void setVillageCoreWorldContext(BlockContext villageCoreWorldContext) {
    this.villageCoreWorldContext = villageCoreWorldContext;
  }

  public AreaFootprint getClaimedArea() {
    return claimedArea;
  }

  public void setClaimedArea(AreaFootprint claimedArea) {
    this.claimedArea = claimedArea;
  }

  public VillageRegistryEntry clone() {
    return VillageRegistryEntryCodec.clone(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof VillageRegistryEntry other)) {
      return false;
    }
    return Objects.equals(this.villageId, other.villageId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.villageId);
  }

}
