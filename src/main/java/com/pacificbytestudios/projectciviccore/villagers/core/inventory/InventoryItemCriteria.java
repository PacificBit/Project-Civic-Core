package com.pacificbytestudios.projectciviccore.villagers.core.inventory;

import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest.ItemRequestCriterion;

public final class InventoryItemCriteria {
  private enum MatchMode {
    EXACT,
    PREFIX,
    SUFFIX,
    CONTAINS,
    PREFIX_AND_SUFFIX,
    ANY
  }

  private final String itemId;
  private final boolean filterIn;
  private final int count;

  private final MatchMode matchMode;
  private final String normalizedPattern;
  private String leftNormalizedPattern;
  private String rightNormalizedPattern;

  public InventoryItemCriteria(String itemId, boolean filterIn, int count) {
    this.itemId = itemId;
    this.filterIn = filterIn;
    this.count = count;

    if (itemId == null || itemId.isBlank() || "*".equals(itemId)) {
      this.matchMode = MatchMode.ANY;
      this.normalizedPattern = "";
      return;
    }

    boolean wildcardStart = itemId.startsWith("*");
    boolean wildcardEnd = itemId.endsWith("*");
    boolean containsWildcard = itemId.contains("*");

    if (!wildcardStart && !wildcardEnd && containsWildcard) {
      this.matchMode = MatchMode.PREFIX_AND_SUFFIX;
      int wildcard = itemId.indexOf('*');
      leftNormalizedPattern = itemId.substring(0, wildcard);
      rightNormalizedPattern = itemId.substring(wildcard + 1);
      this.normalizedPattern = "";
    } else if (wildcardStart && wildcardEnd && itemId.length() > 2) {
      this.matchMode = MatchMode.CONTAINS;
      this.normalizedPattern = itemId.substring(1, itemId.length() - 1);
    } else if (wildcardStart) {
      this.matchMode = MatchMode.SUFFIX;
      this.normalizedPattern = itemId.substring(1);
    } else if (wildcardEnd) {
      this.matchMode = MatchMode.PREFIX;
      this.normalizedPattern = itemId.substring(0, itemId.length() - 1);
    } else {
      this.matchMode = MatchMode.EXACT;
      this.normalizedPattern = itemId;
    }
  }

  public ItemRequestCriterion toVillageRequestCriterion() {
    return new ItemRequestCriterion(this.itemId, this.filterIn, this.count);
  }

  public String getItemId() {
    return this.itemId;
  }

  public boolean isFilterIn() {
    return this.filterIn;
  }

  public int getCount() {
    return this.count;
  }

  public boolean matches(String itemName) {
    if (itemName == null) {
      return false;
    }

    return switch (this.matchMode) {
      case ANY -> true;
      case PREFIX_AND_SUFFIX ->
        itemName.startsWith(this.leftNormalizedPattern) && itemName.endsWith(this.rightNormalizedPattern);
      case EXACT -> itemName.equals(this.normalizedPattern);
      case PREFIX -> itemName.startsWith(this.normalizedPattern);
      case SUFFIX -> itemName.endsWith(this.normalizedPattern);
      case CONTAINS -> itemName.contains(this.normalizedPattern);
    };
  }
}
