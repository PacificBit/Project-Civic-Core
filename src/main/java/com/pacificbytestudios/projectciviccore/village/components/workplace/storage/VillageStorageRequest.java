package com.pacificbytestudios.projectciviccore.village.components.workplace.storage;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.village.enums.VillageStorageRequestType;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.BlockTarget;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryItemCriteria;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SerializableClass
public class VillageStorageRequest {
  public static final BuilderCodec<VillageStorageRequest> CODEC = VillageStorageRequestCodec.CODEC;

  @Setter(lombok.AccessLevel.NONE)
  UUID requestId;
  UUID requesterWorkplaceId;
  UUID storageWorkplaceId;
  VillageStorageRequestType type;
  @Setter(lombok.AccessLevel.NONE)
  ArrayDeque<BlockTarget> targetsWorkplaceStorage = new ArrayDeque<>();

  Set<ItemRequestCriterion> itemCriteria = new HashSet<>();
  @Setter(lombok.AccessLevel.NONE)
  Set<UUID> triedStorageIds = new HashSet<>();
  boolean registeredWithStorage;

  public VillageStorageRequest() {
    this.requestId = UUID.randomUUID();
  }

  public void overrideRequestId(UUID requestId) {
    this.requestId = requestId;
  }

  public void addItemCriteria(ItemRequestCriterion itemCriteria) {
    this.itemCriteria.add(itemCriteria);
  }

  public void setTriedStorageIds(Set<UUID> triedStorageIds) {
    this.triedStorageIds = triedStorageIds != null ? new HashSet<>(triedStorageIds) : new HashSet<>();
  }

  public void addTriedStorageId(UUID storageWorkplaceId) {
    if (storageWorkplaceId == null) {
      return;
    }

    this.triedStorageIds.add(storageWorkplaceId);
  }

  public void clearTriedStorageIds() {
    this.triedStorageIds.clear();
  }

  public void setTargetsWorkplaceStorage(Collection<BlockTarget> targetWorkplaceStorage) {
    this.targetsWorkplaceStorage = targetWorkplaceStorage != null
        ? new ArrayDeque<>(targetWorkplaceStorage)
        : new ArrayDeque<>();
  }

  @Override
  public int hashCode() {
    return Objects.hash(requestId, requesterWorkplaceId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof VillageStorageRequest other))
      return false;
    return this.targetsWorkplaceStorage.equals(other.targetsWorkplaceStorage)
        && Objects.equals(this.requestId, other.requestId);
  }

  @Override
  public VillageStorageRequest clone() {
    return VillageStorageRequestCodec.clone(this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("VillageStorageRequest\n");
    sb.append("requestId=").append(this.requestId).append("\n");
    sb.append("requesterWorkplaceId=").append(this.requesterWorkplaceId).append("\n");
    sb.append("storageWorkplaceId=").append(this.storageWorkplaceId).append("\n");
    sb.append("triedStorageIds=").append(this.triedStorageIds).append("\n");
    sb.append("registeredWithStorage=").append(this.registeredWithStorage).append("\n");
    sb.append("type=").append(this.type).append("\n");
    sb.append("targetWorkplaceStorage=").append(this.targetsWorkplaceStorage).append("\n");
    sb.append("itemCriteria=");

    if (this.itemCriteria == null || this.itemCriteria.isEmpty()) {
      sb.append("[]");
      return sb.toString();
    }

    sb.append("\n");
    for (ItemRequestCriterion criterion : this.itemCriteria) {
      sb.append("- ").append(criterion).append("\n");
    }

    return sb.toString().trim();
  }

  @SerializableClass
  public static class ItemRequestCriterion {
    public static final BuilderCodec<ItemRequestCriterion> CODEC = ItemRequestCriterionCodec.CODEC;

    @Getter
    @Setter
    String attributeKey;
    @Getter
    @Setter
    boolean filterIn = true;
    @Getter
    @Setter
    int requiredAmount;

    public ItemRequestCriterion() {
    }

    public ItemRequestCriterion(String attributeKey, boolean filterIn, int requiredAmount) {
      this.attributeKey = attributeKey;
      this.filterIn = filterIn;
      this.requiredAmount = requiredAmount;
    }

    public ItemRequestCriterion clone() {
      return ItemRequestCriterionCodec.clone(this);
    }

    public InventoryItemCriteria toCriteria() {
      return new InventoryItemCriteria(this.attributeKey, this.filterIn, this.requiredAmount);
    }

    @Override
    public String toString() {
      return "attributeKey=" + this.attributeKey
          + ", filterIn=" + this.filterIn
          + ", requiredAmount=" + this.requiredAmount;
    }

    @Override
    public int hashCode() {
      return Objects.hash(attributeKey, filterIn, requiredAmount);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (!(obj instanceof ItemRequestCriterion other))
        return false;
      return Objects.equals(this.attributeKey, other.attributeKey)
          && this.filterIn == other.filterIn
          && this.requiredAmount == other.requiredAmount;
    }

  }

}
