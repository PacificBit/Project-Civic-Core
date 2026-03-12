package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general;

import java.util.ArrayDeque;
import java.util.List;
import java.util.function.Supplier;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.InventoryHelper;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderInteractWithExternalInventory;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver.ResolvedContainer;

public class InteractWithExternalInventory extends ActionBase {
  private final String snapshotKey;
  private final ActionType actionType;
  private final List<ItemCriteria> itemCriteria;

  public InteractWithExternalInventory(
      BuilderInteractWithExternalInventory builderActionBase,
      BuilderSupport support) {
    super(builderActionBase);
    this.snapshotKey = builderActionBase.getSnapshotKey();
    this.actionType = builderActionBase.getActionType(support);
    // TODO: should use the criteria given by the workplace too
    this.itemCriteria = builderActionBase.getItemCriteria(support);
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

    VillagerWorkerComponent workerData = villager.getWorkerData();
    if (workerData == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[InteractWithExternalInventory] Could not fetch worker data");
      return false;
    }

    WorkplaceAreaSnapshot snapshot = workerData.getWorkareaSnapshot();
    if (snapshot == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[InteractWithExternalInventory] Could not fetch snapshot data");
      return false;
    }

    ArrayDeque<Vector3i> coords = snapshot.getBlockTypePosition().get(this.snapshotKey);
    if (coords.size() < 1) {
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

    // TODO: Don't forget about the workplace criteria
    if (this.itemCriteria == null) {
      return this.transferAllItemStacks(
          chestContainer,
          npcStorage);
    }

    if (this.actionType.equals(ActionType.WITHDRAW)) {
      return this.transferRequestedItems(chestContainer, npcStorage);
    }
    return this.transferRequestedItems(npcStorage, chestContainer);
  }

  // TODO: what should happen if not all the items where removed?
  // I should use the other sensor i created to interact with inventories (tho i
  // need to update it)
  private boolean transferAllItemStacks(ItemContainer container, ItemContainer npcStorage) {
    int contains, moved;

    if (this.actionType.equals(ActionType.WITHDRAW)) {
      contains = container.getCapacity() - InventoryHelper.countFreeSlots(container);
      moved = container.moveAllItemStacksTo(npcStorage).size();
    } else {
      contains = npcStorage.getCapacity() - InventoryHelper.countFreeSlots(npcStorage);
      moved = npcStorage.moveAllItemStacksTo(container).size();
    }

    return (contains - moved) == 0;
  }

  private boolean transferRequestedItems(ItemContainer from, ItemContainer to) {
    // InventoryHelper.matchesItem(List<String> names, itemStack)
    // InventoryHelper.containsItem(inventory, name)
    // InventoryHelper.countItems(inventory, List<String> Names)

    return false;
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

  public static class ItemCriteria {
    private final String itemId;
    private final boolean filterIn;
    private final int count;

    public ItemCriteria(String itemId, boolean filterIn, int count) {
      this.itemId = itemId;
      this.filterIn = filterIn;
      this.count = count;
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
  }
}
