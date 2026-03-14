package com.pacificbytestudios.projectciviccore.village.components.workplace.storage;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.math.vector.Vector3i;
import com.pacificbytestudios.projectciviccore.codecs.ArrayDequeCodec;
import com.pacificbytestudios.projectciviccore.village.enums.VillageStorageRequestType;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryItemCriteria;

public class VillageStorageRequest {
  public static final BuilderCodec<VillageStorageRequest> CODEC = BuilderCodec
      .builder(VillageStorageRequest.class, VillageStorageRequest::new)

      .append(new KeyedCodec<>("RequestId", Codec.UUID_BINARY),
          (obj, value) -> obj.requestId = value,
          obj -> obj.requestId)
      .add()

      .append(new KeyedCodec<>("RequesterWorkplaceId", Codec.UUID_BINARY),
          (obj, value) -> obj.requesterWorkplaceId = value,
          obj -> obj.requesterWorkplaceId)
      .add()

      .append(new KeyedCodec<>("Type", new EnumCodec<>(VillageStorageRequestType.class)),
          (obj, value) -> obj.type = value,
          obj -> obj.type)
      .add()

      .append(new KeyedCodec<>("TargetsWorkplaceStorage", new ArrayDequeCodec<Vector3i>(Vector3i.CODEC, "Target")),
          (obj, value) -> obj.targetsWorkplaceStorage = value,
          obj -> obj.targetsWorkplaceStorage)
      .add()

      .append(new KeyedCodec<Set<ItemRequestCriterion>>(
          "ItemCriteria",
          new SetCodec<ItemRequestCriterion, HashSet<ItemRequestCriterion>>(
              ItemRequestCriterion.CODEC,
              HashSet::new,
              false)),
          (obj, value) -> obj.itemCriteria = value,
          obj -> obj.itemCriteria)
      .add()

      .build();

  private UUID requestId;
  private UUID requesterWorkplaceId;
  private VillageStorageRequestType type;
  private ArrayDeque<Vector3i> targetsWorkplaceStorage = new ArrayDeque<>();

  private Set<ItemRequestCriterion> itemCriteria = new HashSet<>();

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

  public ArrayDeque<Vector3i> getTargetsWorkplaceStorage() {
    return targetsWorkplaceStorage;
  }

  public void setTargetsWorkplaceStorage(ArrayDeque<Vector3i> targetWorkplaceStorage) {
    this.targetsWorkplaceStorage = targetWorkplaceStorage;
  }

  public void setTargetsWorkplaceStorage(List<Vector3i> targetWorkplaceStorage) {
    this.targetsWorkplaceStorage.addAll(targetWorkplaceStorage);
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
    VillageStorageRequest request = new VillageStorageRequest();
    request.requestId = this.requestId;
    request.requesterWorkplaceId = this.requesterWorkplaceId;
    request.type = this.type;
    request.targetsWorkplaceStorage = this.targetsWorkplaceStorage != null ? this.targetsWorkplaceStorage.clone()
        : null;
    request.itemCriteria = new HashSet<>();

    for (ItemRequestCriterion criterion : this.itemCriteria) {
      request.itemCriteria.add(criterion != null ? criterion.clone() : null);
    }

    return request;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("VillageStorageRequest\n");
    sb.append("requestId=").append(this.requestId).append("\n");
    sb.append("requesterWorkplaceId=").append(this.requesterWorkplaceId).append("\n");
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
