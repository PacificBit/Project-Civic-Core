package com.pacificbytestudios.projectciviccore.village.components.workplace.storage;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.village.enums.VillageStorageRequestType;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryItemCriteria;

@SerializableClass
public class VillageStorageRequest {
  public static final BuilderCodec<VillageStorageRequest> CODEC = VillageStorageRequestCodec.CODEC;

  UUID requestId;
  UUID requesterWorkplaceId;
  UUID storageWorkplaceId;
  VillageStorageRequestType type;
  ArrayDeque<Vec3f> targetsWorkplaceStorage = new ArrayDeque<>();

  Set<ItemRequestCriterion> itemCriteria = new HashSet<>();
  Set<UUID> triedStorageIds = new HashSet<>();
  boolean registeredWithStorage;

  public VillageStorageRequest() {
    this.requestId = UUID.randomUUID();
  }

  public void overrideRequestId(UUID requestId) {
    this.requestId = requestId;
  }

  public UUID getRequestId() {
    return this.requestId;
  }

  public UUID getRequesterWorkplaceId() {
    return this.requesterWorkplaceId;
  }

  public void setRequesterWorkplaceId(UUID requesterWorkplaceId) {
    this.requesterWorkplaceId = requesterWorkplaceId;
  }

  public UUID getStorageWorkplaceId() {
    return this.storageWorkplaceId;
  }

  public void setStorageWorkplaceId(UUID storageWorkplaceId) {
    this.storageWorkplaceId = storageWorkplaceId;
  }

  public VillageStorageRequestType getType() {
    return this.type;
  }

  public void setType(VillageStorageRequestType type) {
    this.type = type;
  }

  public Set<ItemRequestCriterion> getItemCriteria() {
    return itemCriteria;
  }

  public void setItemCriteria(Set<ItemRequestCriterion> itemCriteria) {
    this.itemCriteria = itemCriteria;
  }

  public void addItemCriteria(ItemRequestCriterion itemCriteria) {
    this.itemCriteria.add(itemCriteria);
  }

  public Set<UUID> getTriedStorageIds() {
    return this.triedStorageIds;
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

  public boolean isRegisteredWithStorage() {
    return this.registeredWithStorage;
  }

  public void setRegisteredWithStorage(boolean registeredWithStorage) {
    this.registeredWithStorage = registeredWithStorage;
  }

  public ArrayDeque<Vec3f> getTargetsWorkplaceStorage() {
    return targetsWorkplaceStorage;
  }

  public void setTargetsWorkplaceStorage(Collection<Vec3f> targetWorkplaceStorage) {
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

  public static class ItemRequestCriterion {
    public static final BuilderCodec<ItemRequestCriterion> CODEC = BuilderCodec
        .builder(ItemRequestCriterion.class, ItemRequestCriterion::new)

        .append(new KeyedCodec<>("AttributeKey", Codec.STRING),
            (obj, value) -> obj.attributeKey = value,
            obj -> obj.attributeKey)
        .add()

        .append(new KeyedCodec<>("FilterIn", Codec.BOOLEAN, false),
            (obj, value) -> obj.filterIn = value,
            obj -> obj.filterIn)
        .add()

        .append(new KeyedCodec<>("RequiredAmount", Codec.INTEGER),
            (obj, value) -> obj.requiredAmount = value,
            obj -> obj.requiredAmount)
        .add()

        .build();

    private String attributeKey;
    private boolean filterIn = true;
    private int requiredAmount;

    public ItemRequestCriterion() {
    }

    public ItemRequestCriterion(String attributeKey, boolean filterIn, int requiredAmount) {
      this.attributeKey = attributeKey;
      this.filterIn = filterIn;
      this.requiredAmount = requiredAmount;
    }

    public String getAttributeKey() {
      return attributeKey;
    }

    public void setAttributeKey(String attributeKey) {
      this.attributeKey = attributeKey;
    }

    public boolean isFilterIn() {
      return filterIn;
    }

    public void setFilterIn(boolean filterIn) {
      this.filterIn = filterIn;
    }

    public int getRequiredAmount() {
      return requiredAmount;
    }

    public void setRequiredAmount(int requiredAmount) {
      this.requiredAmount = requiredAmount;
    }

    public ItemRequestCriterion clone() {
      ItemRequestCriterion criterion = new ItemRequestCriterion();
      criterion.attributeKey = this.attributeKey;
      criterion.filterIn = this.filterIn;
      criterion.requiredAmount = this.requiredAmount;
      return criterion;
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
