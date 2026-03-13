package com.pacificbytestudios.projectciviccore.villagers.core.inventory;

import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.ItemStack;
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

  public static boolean isTransferableItem(ItemStack item) {
    return item != null && !item.isBroken();
  }

  public static int getTransferableAmount(ItemStack item, int requestedAmount) {
    if (!isTransferableItem(item) || requestedAmount <= 0) {
      return 0;
    }

    return Math.min(requestedAmount, item.getQuantity());
  }
}
