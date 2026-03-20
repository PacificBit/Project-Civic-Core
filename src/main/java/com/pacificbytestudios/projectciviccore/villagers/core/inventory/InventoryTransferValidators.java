package com.pacificbytestudios.projectciviccore.villagers.core.inventory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest.ItemRequestCriterion;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.WorkplaceVillageStorageRequestComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;

public final class InventoryTransferValidators {

  public record ResolvedCriteria(List<InventoryItemCriteria> criteria, UUID assignedRequestId) {
    public static ResolvedCriteria empty() {
      return new ResolvedCriteria(List.of(), null);
    }
  }

  private InventoryTransferValidators() {
  }

  public static ResolvedCriteria resolveAssignedStorageRequestCriteria(
      VillagerWorkerComponent workerData,
      UUID workerId) {

    if (workerData == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[InventoryTransferValidators] Could not fetch worker data");
      return ResolvedCriteria.empty();
    }

    WorkplaceComponent workplaceComponent = workerData.getWorkplace();
    if (workplaceComponent == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[InventoryTransferValidators] Could not fetch workplace component");
      return ResolvedCriteria.empty();
    }

    Ref<ChunkStore> workplaceRef = workplaceComponent.getWorkplaceRef().getRef();
    if (workplaceRef == null || !workplaceRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[InventoryTransferValidators] Could not fetch workplace ref");
      return ResolvedCriteria.empty();
    }

    Store<ChunkStore> chunkStore = workplaceRef.getStore();
    WorkplaceVillageStorageRequestComponent storageComponent = chunkStore.getComponent(
        workplaceRef,
        WorkplaceVillageStorageRequestComponent.getComponentType());

    if (storageComponent == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[InventoryTransferValidators] Could not fetch village storage component");
      return ResolvedCriteria.empty();
    }

    ArrayDeque<VillageStorageRequest> requestQueue = storageComponent.getAssignedRequest(workerId);

    if (requestQueue == null || requestQueue.isEmpty()) {
      ProjectCivicCore.LOGGER.atInfo()
          .log("[InventoryTransferValidators] No request criteria found");
      return ResolvedCriteria.empty();
    }

    VillageStorageRequest request = requestQueue.iterator().next();

    List<InventoryItemCriteria> criteria = request.getItemCriteria()
        .stream()
        .sorted((left, right) -> {
          int attributeCompare = String.valueOf(left.getAttributeKey())
              .compareTo(String.valueOf(right.getAttributeKey()));
          if (attributeCompare != 0) {
            return attributeCompare;
          }

          int filterCompare = Boolean.compare(left.isFilterIn(), right.isFilterIn());
          if (filterCompare != 0) {
            return filterCompare;
          }

          return Integer.compare(left.getRequiredAmount(), right.getRequiredAmount());
        })
        .map(ItemRequestCriterion::toCriteria)
        .toList();

    return new ResolvedCriteria(criteria, request.getRequestId());
  }

  public static ToIntFunction<ItemStack> buildTransferValidator(
      List<InventoryItemCriteria> itemCriteria,
      IntUnaryOperator committedAmountLookup) {

    if (itemCriteria == null || itemCriteria.isEmpty()) {
      return item -> getTransferableAmount(
          item,
          item != null ? item.getQuantity() : 0);
    }

    Predicate<String> isExcluded = name -> false;
    Predicate<String> isIncluded = name -> false;
    boolean hasInclusiveRules = false;

    for (InventoryItemCriteria criteria : itemCriteria) {
      if (!criteria.isFilterIn()) {
        isExcluded = isExcluded.or(criteria::matches);
      } else {
        isIncluded = isIncluded.or(criteria::matches);
        hasInclusiveRules = true;
      }
    }

    final boolean requiresInclusion = hasInclusiveRules;
    final Predicate<String> fastRejectionCheck = isExcluded;
    final Predicate<String> fastAcceptanceCheck = isIncluded;

    return item -> {
      int requestedAmount = getTransferableAmount(
          item,
          item != null ? item.getQuantity() : 0);
      if (requestedAmount <= 0) {
        return 0;
      }

      String itemName = item.getItemId();

      if (fastRejectionCheck.test(itemName)) {
        return 0;
      }

      if (requiresInclusion && !fastAcceptanceCheck.test(itemName)) {
        return 0;
      }

      if (!requiresInclusion) {
        return requestedAmount;
      }

      for (int i = 0; i < itemCriteria.size(); i++) {
        InventoryItemCriteria criteria = itemCriteria.get(i);

        if (criteria.isFilterIn() && criteria.matches(itemName)) {
          if (criteria.getCount() <= 0) {
            return requestedAmount;
          }

          int alreadyCommitted = committedAmountLookup != null
              ? committedAmountLookup.applyAsInt(i)
              : 0;

          int remainingAllowed = criteria.getCount() - alreadyCommitted;

          if (remainingAllowed <= 0) {
            return 0;
          }

          return Math.min(requestedAmount, remainingAllowed);
        }
      }

      return 0;
    };
  }

  public static List<InventoryItemCriteria> getMissingCriteria(
      ItemContainer container,
      List<InventoryItemCriteria> itemCriteria) {

    if (container == null) {
      return itemCriteria != null ? itemCriteria : List.of();
    }

    if (itemCriteria == null || itemCriteria.isEmpty() || !hasLimitedItemCriteria(itemCriteria)) {
      return List.of();
    }

    Map<Integer, Integer> committedAmounts = new HashMap<>();
    ToIntFunction<ItemStack> validator = buildTransferValidator(
        itemCriteria,
        criterionIndex -> committedAmounts.getOrDefault(criterionIndex, 0));

    for (short slot = 0; slot < container.getCapacity(); slot++) {
      ItemStack item = container.getItemStack(slot);
      if (!isTransferableItem(item)) {
        continue;
      }

      int allowedAmount = validator.applyAsInt(item);
      if (allowedAmount <= 0) {
        continue;
      }

      commitTransferredItemAmount(
          item.getItemId(),
          Math.min(allowedAmount, item.getQuantity()),
          itemCriteria,
          committedAmounts);

      if (hasSatisfiedAllLimitedItemCriteria(itemCriteria, committedAmounts)) {
        return List.of();
      }
    }

    List<InventoryItemCriteria> missingCriteria = new ArrayList<>();
    for (int i = 0; i < itemCriteria.size(); i++) {
      InventoryItemCriteria criteria = itemCriteria.get(i);
      if (!criteria.isFilterIn() || criteria.getCount() <= 0) {
        continue;
      }

      int missingAmount = criteria.getCount() - committedAmounts.getOrDefault(i, 0);
      if (missingAmount > 0) {
        missingCriteria.add(new InventoryItemCriteria(criteria.getItemId(), true, missingAmount));
      }
    }

    return missingCriteria;
  }

  public static boolean containsRequestedItems(
      ItemContainer container,
      List<InventoryItemCriteria> itemCriteria) {
    return getMissingCriteria(container, itemCriteria).isEmpty();
  }

  public static boolean removeRequestedItems(
      ItemContainer container,
      List<InventoryItemCriteria> itemCriteria) {

    if (container == null) {
      return itemCriteria == null || itemCriteria.isEmpty();
    }

    if (itemCriteria == null || itemCriteria.isEmpty() || !hasLimitedItemCriteria(itemCriteria)) {
      return true;
    }

    Map<Integer, Integer> committedAmounts = new HashMap<>();
    ToIntFunction<ItemStack> validator = buildTransferValidator(
        itemCriteria,
        criterionIndex -> committedAmounts.getOrDefault(criterionIndex, 0));

    for (short slot = 0; slot < container.getCapacity(); slot++) {
      ItemStack item = container.getItemStack(slot);
      if (!isTransferableItem(item)) {
        continue;
      }

      int allowedAmount = validator.applyAsInt(item);
      if (allowedAmount <= 0) {
        continue;
      }

      int amountToRemove = Math.min(allowedAmount, item.getQuantity());
      if (!container.removeItemStackFromSlot(slot, amountToRemove, true, true).succeeded()) {
        ProjectCivicCore.LOGGER.atWarning()
            .log("[InventoryTransferValidators] Could not remove requested item stack");
        return false;
      }

      commitTransferredItemAmount(
          item.getItemId(),
          amountToRemove,
          itemCriteria,
          committedAmounts);

      if (hasSatisfiedAllLimitedItemCriteria(itemCriteria, committedAmounts)) {
        return true;
      }
    }

    return hasSatisfiedAllLimitedItemCriteria(itemCriteria, committedAmounts);
  }

  public static boolean isTransferableItem(ItemStack item) {
    return item != null && !item.isBroken();
  }

  public static int getTransferableAmount(ItemStack item, int requestedAmount) {
    if (!isTransferableItem(item) || requestedAmount <= 0) {
      return 0;
    }

    return Math.min(requestedAmount, item.getQuantity());
  }

  private static boolean hasLimitedItemCriteria(List<InventoryItemCriteria> itemCriteria) {
    if (itemCriteria == null || itemCriteria.isEmpty()) {
      return false;
    }

    for (InventoryItemCriteria criteria : itemCriteria) {
      if (criteria.isFilterIn() && criteria.getCount() > 0) {
        return true;
      }
    }

    return false;
  }

  private static boolean hasSatisfiedAllLimitedItemCriteria(
      List<InventoryItemCriteria> itemCriteria,
      Map<Integer, Integer> committedAmounts) {
    if (itemCriteria == null || itemCriteria.isEmpty()) {
      return true;
    }

    boolean hasLimitedCriteria = false;

    for (int i = 0; i < itemCriteria.size(); i++) {
      InventoryItemCriteria criteria = itemCriteria.get(i);
      if (!criteria.isFilterIn() || criteria.getCount() <= 0) {
        continue;
      }

      hasLimitedCriteria = true;
      int committedAmount = committedAmounts.getOrDefault(i, 0);
      if (committedAmount < criteria.getCount()) {
        return false;
      }
    }

    return hasLimitedCriteria;
  }

  private static void commitTransferredItemAmount(
      String itemId,
      int movedAmount,
      List<InventoryItemCriteria> itemCriteria,
      Map<Integer, Integer> committedAmounts) {
    if (itemId == null || movedAmount <= 0 || itemCriteria == null || itemCriteria.isEmpty()) {
      return;
    }

    for (int i = 0; i < itemCriteria.size(); i++) {
      InventoryItemCriteria criteria = itemCriteria.get(i);
      if (criteria.isFilterIn() && criteria.getCount() > 0 && criteria.matches(itemId)) {
        int alreadyCommitted = committedAmounts.getOrDefault(i, 0);
        committedAmounts.put(i, alreadyCommitted + movedAmount);
        return;
      }
    }
  }
}
