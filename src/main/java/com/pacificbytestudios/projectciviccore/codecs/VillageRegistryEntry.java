package com.pacificbytestudios.projectciviccore.codecs;

import java.util.Objects;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class VillageRegistryEntry {
  public static final BuilderCodec<VillageRegistryEntry> CODEC = BuilderCodec
      .builder(VillageRegistryEntry.class, VillageRegistryEntry::new)

      .append(new KeyedCodec<>("VillageId", Codec.UUID_BINARY),
          (obj, value) -> obj.villageId = value,
          (obj) -> obj.villageId)
      .add()

      .append(new KeyedCodec<>("VillageName", Codec.STRING),
          (obj, value) -> obj.villageName = value,
          (obj) -> obj.villageName)
      .add()

      .append(new KeyedCodec<>("VillageContext", BlockContext.CODEC),
          (obj, value) -> obj.villageCoreWorldContext = value,
          (obj) -> obj.villageCoreWorldContext)
      .add()

      .append(new KeyedCodec<>("ClaimedArea", AreaFootprint.CODEC),
          (obj, value) -> obj.claimedArea = value,
          (obj) -> obj.claimedArea)
      .add()

      .build();

  private UUID villageId;
  private String villageName;
  private BlockContext villageCoreWorldContext;
  private AreaFootprint claimedArea;

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
    VillageRegistryEntry entry = new VillageRegistryEntry();
    entry.villageId = this.villageId;
    entry.villageName = this.villageName;
    entry.villageCoreWorldContext = this.villageCoreWorldContext == null ? null : this.villageCoreWorldContext.clone();
    entry.claimedArea = this.claimedArea == null ? null : this.claimedArea.clone();
    return entry;
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
