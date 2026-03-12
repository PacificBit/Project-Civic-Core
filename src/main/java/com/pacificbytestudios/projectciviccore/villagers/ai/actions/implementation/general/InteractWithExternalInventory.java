package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.InventoryHelper;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest.ItemRequestCriterion;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.WorkplaceVillageStorageRequestComponent;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderInteractWithExternalInventory;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver.ResolvedContainer;

public class InteractWithExternalInventory extends ActionBase {
  private final String snapshotKey;
  private final ActionType actionType;
  private final boolean consumeSnapshotTargetOnSuccess;
  private final boolean consumeSnapshotTargetOnFail;
  private final boolean useSnapshotItemCriteria;
  private List<ItemCriteria> itemCriteria;

  private VillagerWorkerComponent workerData;
  private UUID workerId;
  private Map<String, Integer> itemCount;
  private String itemCountContextKey;
  private UUID assignedRequestId;

  public InteractWithExternalInventory(
      BuilderInteractWithExternalInventory builderActionBase,
      BuilderSupport support) {
    super(builderActionBase);
    this.snapshotKey = builderActionBase.getSnapshotKey();
    this.actionType = builderActionBase.getActionType(support);
    this.itemCriteria = builderActionBase.getItemCriteria(support);
    this.consumeSnapshotTargetOnSuccess = builderActionBase.getConsumeSnapshotTargetOnSuccessFlag();
    this.consumeSnapshotTargetOnFail = builderActionBase.getConsumeSnapshotTargetOnFailFlag();
    this.useSnapshotItemCriteria = builderActionBase.getUseSnapshotItemCriteria();
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {

    VillagerComponent villager = store.getComponent(ref,
        VillagerComponent.getComponentType());

    this.workerData = villager.getWorkerData();
    if (this.workerData == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[InteractWithExternalInventory] Could not fetch worker data");
      return false;
    }

    WorkplaceAreaSnapshot snapshot = this.workerData.getWorkareaSnapshot();
    if (snapshot == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[InteractWithExternalInventory] Could not fetch snapshot data");
      return false;
    }

    this.workerId = store.getComponent(ref,
        UUIDComponent.getComponentType()).getUuid();

    ArrayDeque<Vector3i> coords = snapshot.getBlockTypePosition().get(this.snapshotKey);
    if (coords == null || coords.isEmpty()) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[InteractWithExternalInventory] Could not find coords in snapshot for key: " + this.snapshotKey);
      return false;
    }

    Vector3i containerPosition = coords.peek();
    World world = store.getExternalData().getWorld();
    ResolvedContainer resolvedContainer = ContainerResolver.resolveContainerAt(
        world,
        containerPosition);

    if (resolvedContainer == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[InteractWithExternalInventory] Could not resolve container at " +
              containerPosition);
      return false;
    }

    NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());

    SimpleItemContainer chestContainer = resolvedContainer.containerBlock().getItemContainer();
    ItemContainer npcStorage = npcEntity.getInventory().getStorage();

    boolean done = false;
    if (this.itemCriteria == null && !this.useSnapshotItemCriteria) {
      resetTrackedItemCount();
      done = this.transferAllItemStacks(chestContainer, npcStorage);
    } else {
      if (this.actionType.equals(ActionType.WITHDRAW)) {
        done = this.transferRequestedItems(snapshot, chestContainer, npcStorage);
      } else {
        done = this.transferRequestedItems(snapshot, npcStorage, chestContainer);
      }
    }

    if (done) {
      if (shouldClearRemainingTargetsOnSuccess()) {
        coords.clear();
      } else if (this.consumeSnapshotTargetOnSuccess) {
        coords.pollFirst();
      }
    } else if (this.consumeSnapshotTargetOnFail) {
      coords.pollFirst();
    }
    return done;
  }

  private boolean transferAllItemStacks(ItemContainer container, ItemContainer npcStorage) {
    int contains, moved;

    if (this.actionType.equals(ActionType.WITHDRAW)) {
      contains = container.getCapacity() -
          InventoryHelper.countFreeSlots(container);
      moved = container.moveAllItemStacksTo(npcStorage).size();
    } else {
      contains = npcStorage.getCapacity() -
          InventoryHelper.countFreeSlots(npcStorage);
      moved = npcStorage.moveAllItemStacksTo(container).size();
    }

    return (contains - moved) == 0;
  }

  private boolean transferRequestedItems(
      WorkplaceAreaSnapshot snapshot,
      ItemContainer from,
      ItemContainer to) {
    ToIntFunction<ItemStack> validator = buildItemValidator();
    prepareTrackedItemCount(snapshot);
    boolean movedAny = false;

    for (short slot = 0; slot < from.getCapacity(); slot++) {
      ItemStack item = from.getItemStack(slot);
      if (!isTransferableItem(item)) {
        continue;
      }

      int allowedAmount = validator.applyAsInt(item);
      if (allowedAmount <= 0) {
        continue;
      }

      if (allowedAmount >= item.getQuantity()) {
        if (from.moveItemStackFromSlot(slot, to, true, true).succeeded()) {
          commitTransferredItemAmount(item.getItemId(), item.getQuantity());
          movedAny = true;
          continue;
        }
      } else {
        if (from.moveItemStackFromSlot(slot, allowedAmount, to, true, true).succeeded()) {
          commitTransferredItemAmount(item.getItemId(), allowedAmount);
          movedAny = true;
          continue;
        }
      }

      ProjectCivicCore.LOGGER.atWarning().log("[InteractWithExternalInventory] Could not move requested item stack");
      return false;
    }
    return movedAny;
  }

  private ToIntFunction<ItemStack> buildItemValidator() {
    if (this.useSnapshotItemCriteria) {
      return buildStorageRequestItemValidator();
    }
    return buildItemCriteriaValidator();
  }

  private ToIntFunction<ItemStack> buildStorageRequestItemValidator() {
    ToIntFunction<ItemStack> defaultValidator = item -> getTransferableAmount(
        item,
        item != null ? item.getQuantity() : 0);
    WorkplaceComponent workplaceComponent = this.workerData.getWorkplace();
    this.assignedRequestId = null;
    this.itemCriteria = null;

    if (workplaceComponent == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[buildStorageRequestItemValidator] Could not fetch workplace component");
      return defaultValidator;
    }

    Ref<ChunkStore> workplaceRef = workplaceComponent.getWorkplaceRef().getRef();
    if (workplaceRef == null || !workplaceRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[buildStorageRequestItemValidator] Could not fetch workplace ref");
      return defaultValidator;
    }

    Store<ChunkStore> chunkStore = workplaceRef.getStore();
    WorkplaceVillageStorageRequestComponent storageComponent = chunkStore.getComponent(
        workplaceRef,
        WorkplaceVillageStorageRequestComponent.getComponentType());

    if (storageComponent == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[buildStorageRequestItemValidator] Could not fetch village storage component");
      return defaultValidator;
    }

    ArrayDeque<VillageStorageRequest> requestQueue = storageComponent.getAssignedRequest(this.workerId);

    if (requestQueue.size() == 0) {
      ProjectCivicCore.LOGGER.atInfo()
          .log("[buildStorageRequestItemValidator] No request criteria found");
      return defaultValidator;
    }

    VillageStorageRequest request = requestQueue.iterator().next();
    this.assignedRequestId = request.getRequestId();

    this.itemCriteria = request.getItemCriteria()
        .stream()
        .sorted((left, right) -> {
          int attributeCompare = String.valueOf(left.getAttributeKey())
              .compareTo(String.valueOf(right.getAttributeKey()));
          if (attributeCompare != 0) {
            return attributeCompare;
          }

          return Integer.compare(left.getRequiredAmount(), right.getRequiredAmount());
        })
        .map(ItemRequestCriterion::toCriteria)
        .toList();

    return buildItemCriteriaValidator();
  }

  private ToIntFunction<ItemStack> buildItemCriteriaValidator() {
    if (this.itemCriteria == null || this.itemCriteria.isEmpty()) {
      return item -> getTransferableAmount(
          item,
          item != null ? item.getQuantity() : 0);
    }

    // 1. Creiamo le due "super-condizioni" (equivalente al tuo if concatenato)
    Predicate<String> isExcluded = name -> false;
    Predicate<String> isIncluded = name -> false;
    boolean hasInclusiveRules = false;

    for (ItemCriteria c : this.itemCriteria) {
      if (!c.isFilterIn()) {
        // Concatena con || (OR) per le esclusioni
        isExcluded = isExcluded.or(c::matches);
      } else {
        // Concatena con || (OR) per le inclusioni
        isIncluded = isIncluded.or(c::matches);
        hasInclusiveRules = true;
      }
    }

    // Salviamo variabili finali per la lambda
    final boolean requiresInclusion = hasInclusiveRules;
    final Predicate<String> fastRejectionCheck = isExcluded;
    final Predicate<String> fastAcceptanceCheck = isIncluded;

    // 2. Ritorniamo la funzione che viene eseguita per ogni item
    return item -> {
      int requestedAmount = getTransferableAmount(
          item,
          item != null ? item.getQuantity() : 0);
      if (requestedAmount <= 0) {
        return 0;
      }

      String itemName = item.getItemId();

      // -- FASE 1: CONFERMA IMMEDIATA (Il "singolo if" che volevi) --

      // Se l'item matcha anche solo un'esclusione, scartalo subito (short-circuit
      // veloce)
      if (fastRejectionCheck.test(itemName)) {
        return 0;
      }

      // Se ci sono regole di inclusione e NON matcha nessuna di queste, scartalo
      if (requiresInclusion && !fastAcceptanceCheck.test(itemName)) {
        return 0;
      }

      // -- FASE 2: GESTIONE DELL'AMOUNT --

      if (!requiresInclusion) {
        return requestedAmount; // Tutto accettato senza limiti
      }

      // Dato che sappiamo già che l'item E' ACCETTATO, cerchiamo solo quale regola
      // ha i limiti di "count" da applicare.
      for (int i = 0; i < this.itemCriteria.size(); i++) {
        ItemCriteria criteria = this.itemCriteria.get(i);

        if (criteria.isFilterIn() && criteria.matches(itemName)) {
          if (criteria.getCount() <= 0) {
            return requestedAmount; // Nessun limite per questa regola
          }

          String counterKey = buildCriterionCounterKey(i);
          int alreadyCommitted = this.itemCount.getOrDefault(counterKey, 0);
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

  private boolean isTransferableItem(ItemStack item) {
    return item != null && !item.isBroken();
  }

  private int getTransferableAmount(ItemStack item, int requestedAmount) {
    if (!isTransferableItem(item) || requestedAmount <= 0) {
      return 0;
    }

    return Math.min(requestedAmount, item.getQuantity());
  }

  private void prepareTrackedItemCount(WorkplaceAreaSnapshot snapshot) {
    if (!hasLimitedItemCriteria()) {
      resetTrackedItemCount();
      return;
    }

    String currentContextKey = buildItemCountContextKey(snapshot);
    if (this.itemCount == null || !currentContextKey.equals(this.itemCountContextKey)) {
      this.itemCount = new HashMap<>();
      this.itemCountContextKey = currentContextKey;
    }
  }

  private boolean hasLimitedItemCriteria() {
    if (this.itemCriteria == null || this.itemCriteria.isEmpty()) {
      return false;
    }

    for (ItemCriteria criteria : this.itemCriteria) {
      if (criteria.isFilterIn() && criteria.getCount() > 0) {
        return true;
      }
    }

    return false;
  }

  private String buildItemCountContextKey(WorkplaceAreaSnapshot snapshot) {
    String snapshotIdentifier = snapshot != null && snapshot.getCapturedAt() != null
        ? snapshot.getCapturedAt().toString()
        : "snapshot:" + System.identityHashCode(snapshot);

    String requestIdentifier = this.assignedRequestId != null
        ? this.assignedRequestId.toString()
        : "request:none";

    return this.workerId
        + "|"
        + this.snapshotKey
        + "|"
        + this.actionType.get()
        + "|"
        + snapshotIdentifier
        + "|"
        + requestIdentifier
        + "|"
        + buildItemCriteriaSignature();
  }

  private String buildItemCriteriaSignature() {
    if (this.itemCriteria == null || this.itemCriteria.isEmpty()) {
      return "criteria:none";
    }

    StringBuilder signature = new StringBuilder();
    for (ItemCriteria criteria : this.itemCriteria) {
      signature.append(criteria.getItemId())
          .append(':')
          .append(criteria.isFilterIn())
          .append(':')
          .append(criteria.getCount())
          .append(';');
    }

    return signature.toString();
  }

  private void resetTrackedItemCount() {
    this.itemCount = null;
    this.itemCountContextKey = null;
    this.assignedRequestId = null;
  }

  private boolean shouldClearRemainingTargetsOnSuccess() {
    return this.useSnapshotItemCriteria && hasSatisfiedAllLimitedItemCriteria();
  }

  private boolean hasSatisfiedAllLimitedItemCriteria() {
    if (this.itemCount == null || this.itemCriteria == null || this.itemCriteria.isEmpty()) {
      return false;
    }

    boolean hasLimitedCriteria = false;

    for (int i = 0; i < this.itemCriteria.size(); i++) {
      ItemCriteria criteria = this.itemCriteria.get(i);
      if (!criteria.isFilterIn() || criteria.getCount() <= 0) {
        continue;
      }

      hasLimitedCriteria = true;
      String counterKey = buildCriterionCounterKey(i);
      int committedAmount = this.itemCount.getOrDefault(counterKey, 0);

      if (committedAmount < criteria.getCount()) {
        return false;
      }
    }

    return hasLimitedCriteria;
  }

  private void commitTransferredItemAmount(String itemName, int movedAmount) {
    if (itemName == null || movedAmount <= 0 || this.itemCount == null || this.itemCriteria == null ||
        this.itemCriteria.isEmpty()) {
      return;
    }

    // Cerchiamo a quale regola inclusiva appartiene per aggiornare il count
    for (int i = 0; i < this.itemCriteria.size(); i++) {
      ItemCriteria criteria = this.itemCriteria.get(i);

      // Ignoriamo le esclusioni, ci interessano solo i contatori delle inclusioni
      if (criteria.isFilterIn() && criteria.matches(itemName)) {

        if (criteria.getCount() > 0) {
          String counterKey = buildCriterionCounterKey(i);
          int alreadyCommitted = this.itemCount.getOrDefault(counterKey, 0);
          this.itemCount.put(counterKey, alreadyCommitted + movedAmount);
        }

        // Appena troviamo la regola e aggiorniamo, usciamo.
        return;
      }
    }
  }

  private String buildCriterionCounterKey(int criterionIndex) {
    return "__criterion__" + criterionIndex;
  }

  public enum ActionType implements Supplier<String> {
    DEPOSIT("deposit"),
    WITHDRAW("withdraw");

    private final String type;

    ActionType(String type) {
      this.type = type;
    }

    @Override
    public String get() {
      return this.type;
    }
  }

  private enum MatchMode {
    EXACT,
    PREFIX,
    SUFFIX,
    CONTAINS,
    ANY
  }

  public static class ItemCriteria {
    private final String itemId;
    private final boolean filterIn;
    private final int count;

    private final MatchMode matchMode;
    private final String normalizedPattern;

    public ItemCriteria(String itemId, boolean filterIn, int count) {
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

      if (wildcardStart && wildcardEnd && itemId.length() > 2) {
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

    public String getItemId() {
      return this.itemId;
    }

    public boolean isFilterIn() {
      return this.filterIn;
    }

    public int getCount() {
      return this.count;
    }

    private boolean matches(String itemName) {
      if (itemName == null) {
        return false;
      }

      return switch (this.matchMode) {
        case ANY -> true;
        case EXACT -> itemName.equals(this.normalizedPattern);
        case PREFIX -> itemName.startsWith(this.normalizedPattern);
        case SUFFIX -> itemName.endsWith(this.normalizedPattern);
        case CONTAINS -> itemName.contains(this.normalizedPattern);
      };
    }
  }
}
