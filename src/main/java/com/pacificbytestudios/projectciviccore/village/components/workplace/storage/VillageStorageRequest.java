package com.pacificbytestudios.projectciviccore.village.components.workplace.storage;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.pacificbytestudios.projectciviccore.village.enums.VillageStorageRequestType;

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
  private Set<ItemRequestCriterion> itemCriteria = new HashSet<>();

  public VillageStorageRequest() {
    this.requestId = UUID.randomUUID();
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

  @Override
  public VillageStorageRequest clone() {
    VillageStorageRequest request = new VillageStorageRequest();
    request.requestId = this.requestId;
    request.requesterWorkplaceId = this.requesterWorkplaceId;
    request.type = this.type;
    request.itemCriteria = new HashSet<>();

    for (ItemRequestCriterion criterion : this.itemCriteria) {
      request.itemCriteria.add(criterion != null ? criterion.clone() : null);
    }

    return request;
  }

  public static class ItemRequestCriterion {
    public static final BuilderCodec<ItemRequestCriterion> CODEC = BuilderCodec
        .builder(ItemRequestCriterion.class, ItemRequestCriterion::new)

        .append(new KeyedCodec<>("AttributeKey", Codec.STRING),
            (obj, value) -> obj.attributeKey = value,
            obj -> obj.attributeKey)
        .add()

        .append(new KeyedCodec<>("ExactMatch", Codec.BOOLEAN),
            (obj, value) -> obj.exactMatch = value,
            obj -> obj.exactMatch)
        .add()

        .append(new KeyedCodec<>("RequiredAmount", Codec.INTEGER),
            (obj, value) -> obj.requiredAmount = value,
            obj -> obj.requiredAmount)
        .add()

        .build();

    private String attributeKey;
    private boolean exactMatch = true;
    private int requiredAmount;

    public String getAttributeKey() {
      return attributeKey;
    }

    public void setAttributeKey(String attributeKey) {
      this.attributeKey = attributeKey;
    }

    public boolean isExactMatch() {
      return exactMatch;
    }

    public void setExactMatch(boolean exactMatch) {
      this.exactMatch = exactMatch;
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
      criterion.exactMatch = this.exactMatch;
      criterion.requiredAmount = this.requiredAmount;
      return criterion;
    }
  }

}
