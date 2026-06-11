package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.InventoryHelper;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderInteractWithExternalInventory;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver.ResolvedContainer;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryItemCriteria;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryTransferValidators;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class InteractWithExternalInventory extends VillagerActionBase {
  private final String snapshotKey;
  private final ActionType actionType;
  private final boolean consumeSnapshotTargetOnSuccess;
  private final boolean consumeSnapshotTargetOnFail;
  private final boolean useSnapshotItemCriteria;

  private List<InventoryItemCriteria> itemCriteria;
  private Map<Integer, Integer> itemCount;

  private VillagerWorkerComponent workerData;
  private UUID workerId;
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
      ExecutionSupport executionSupport,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    this.workerData = this.getWorkerDataComponent(ref, store);
    WorkplaceAreaSnapshot snapshot = this.getAreaSnapshot(ref, store);
    NPCEntity npcEntity = this.getNPCEntity(ref, store);
    if (this.workerData == null || snapshot == null || npcEntity == null) {
      return false;
    }

    UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
    this.workerId = uuidComponent.getUuid();

    ArrayDeque<Vec3i> coords = snapshot.getBlockTypePosition().get(this.snapshotKey);
    if (coords == null || coords.isEmpty()) {
      Logger.logWarning("Could not find coords in snapshot for key: " + this.snapshotKey);
      return false;
    }

    Vec3i containerPosition = coords.peek();
    World world = store.getExternalData().getWorld();
    ResolvedContainer resolvedContainer = ContainerResolver.resolveContainerAt(world, containerPosition);

    if (resolvedContainer == null || resolvedContainer.containerBlock() == null) {
      Logger.logWarning("Could not resolve container at " + containerPosition);
      return false;
    }

    SimpleItemContainer chestContainer = resolvedContainer.containerBlock().getItemContainer();
    ItemContainer npcStorage = this.getVillagerStorage(ref, store);
    if (npcStorage == null) {
      return false;
    }

    boolean done;
    if ((this.itemCriteria == null || this.itemCriteria.isEmpty()) && !this.useSnapshotItemCriteria) {
      resetTrackedItemCount();
      done = this.transferAllItemStacks(chestContainer, npcStorage);
    } else if (this.actionType.equals(ActionType.WITHDRAW)) {
      done = this.transferRequestedItems(snapshot, chestContainer, npcStorage);
    } else {
      done = this.transferRequestedItems(snapshot, npcStorage, chestContainer);
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
    int contains;
    int moved;

    if (this.actionType.equals(ActionType.WITHDRAW)) {
      contains = container.getCapacity() - InventoryHelper.countFreeSlots(container);
      moved = container.moveAllItemStacksTo(npcStorage).size();
    } else {
      contains = npcStorage.getCapacity() - InventoryHelper.countFreeSlots(npcStorage);
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
      if (!InventoryTransferValidators.isTransferableItem(item)) {
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

      Logger.logWarning("Could not move requested item stack");
      return false;
    }

    return movedAny;
  }

  private ToIntFunction<ItemStack> buildItemValidator() {
    if (this.useSnapshotItemCriteria) {
      InventoryTransferValidators.ResolvedCriteria resolved = InventoryTransferValidators
          .resolveAssignedStorageRequestCriteria(
              this.workerData,
              this.workerId);

      this.assignedRequestId = resolved.assignedRequestId();
      this.itemCriteria = resolved.criteria();
    } else {
      this.assignedRequestId = null;
    }

    return InventoryTransferValidators.buildTransferValidator(
        this.itemCriteria,
        criterionIndex -> this.itemCount != null
            ? this.itemCount.getOrDefault(criterionIndex, 0)
            : 0);
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

    for (InventoryItemCriteria criteria : this.itemCriteria) {
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

    if (this.assignedRequestId != null) {
      return snapshotIdentifier + "|" + this.assignedRequestId;
    }

    return snapshotIdentifier;
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
      InventoryItemCriteria criteria = this.itemCriteria.get(i);
      if (!criteria.isFilterIn() || criteria.getCount() <= 0) {
        continue;
      }

      hasLimitedCriteria = true;
      int committedAmount = this.itemCount.getOrDefault(i, 0);

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

    for (int i = 0; i < this.itemCriteria.size(); i++) {
      InventoryItemCriteria criteria = this.itemCriteria.get(i);

      if (criteria.isFilterIn() && criteria.matches(itemName)) {
        if (criteria.getCount() > 0) {
          int alreadyCommitted = this.itemCount.getOrDefault(i, 0);
          this.itemCount.put(i, alreadyCommitted + movedAmount);
        }
        return;
      }
    }
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
}
