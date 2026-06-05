package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.InventoryHelper;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderEquipItemFromInventoryAction;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.VillagerInventoryAccess;

public class EquipItemFromInventoryAction extends ActionBase {
  private String itemIdToEquip;

  public EquipItemFromInventoryAction(BuilderEquipItemFromInventoryAction builderActionBase, BuilderSupport support) {
    super(builderActionBase);
    this.itemIdToEquip = builderActionBase.getItemIdToEquip(support);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    super.execute(ref, role, sensorInfo, deltaTime, store);

    NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
    if (npcEntity == null) {
      return false;
    }

    ItemContainer inventory = VillagerInventoryAccess.combinedStorageFirst(store, ref);
    byte activeHotbarSlot = VillagerInventoryAccess.activeHotbarSlot(store, ref);
    ItemContainer hotbar = VillagerInventoryAccess.hotbarContainer(store, ref);
    ItemStack itemInHand = VillagerInventoryAccess.activeHotbarItem(store, ref);
    if (inventory == null || hotbar == null || activeHotbarSlot < 0) {
      return false;
    }

    if (this.itemIdToEquip.toLowerCase().equals("null")) {
      return this.unequipItem(itemInHand, store, ref, hotbar, activeHotbarSlot);
    }

    if (itemInHand != null && itemInHand.getItemId().startsWith(this.itemIdToEquip)) {
      return false;
    }

    return swapToItemFromInventory(inventory, activeHotbarSlot, hotbar, itemInHand);
  }

  private boolean swapToItemFromInventory(ItemContainer inventory, byte activeHotbarSlot, ItemContainer hotbar,
      ItemStack itemInHand) {
    for (short slot = 0; slot < inventory.getCapacity(); slot++) {
      ItemStack itemStack = inventory.getItemStack(slot);
      if (itemStack != null && itemStack.getItemId().startsWith(this.itemIdToEquip)) {

        hotbar.setItemStackForSlot(
            activeHotbarSlot,
            itemStack);

        inventory.setItemStackForSlot(slot, itemInHand);
        return true;
      }
    }

    return false;
  }

  private boolean unequipItem(
      ItemStack itemInHand,
      Store<EntityStore> store,
      Ref<EntityStore> ref,
      ItemContainer hotbar,
      short activeHotbarSlot) {
    if (itemInHand == null) {
      return true;
    }

    ItemContainer storage = VillagerInventoryAccess.storage(store, ref);
    if (storage == null) {
      return false;
    }
    if (InventoryHelper.countFreeSlots(storage) < 1) {
      Logger.logInfo("Storage is full, cannot free the hand slot");
      return false;
    }
    for (short slot = (short) (storage.getCapacity() - 1); slot >= 0; slot--) {
      if (storage.getItemStack(slot) == null) {
        storage.setItemStackForSlot(slot, itemInHand);
        hotbar.setItemStackForSlot(
            activeHotbarSlot,
            null);
        return true;
      }
    }
    return false;
  }
}
