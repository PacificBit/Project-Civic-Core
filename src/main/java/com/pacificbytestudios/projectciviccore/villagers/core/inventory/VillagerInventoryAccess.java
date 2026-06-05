package com.pacificbytestudios.projectciviccore.villagers.core.inventory;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class VillagerInventoryAccess {
  private VillagerInventoryAccess() {
  }

  public static ItemContainer storage(
      ComponentAccessor<EntityStore> accessor,
      Ref<EntityStore> ref) {
    InventoryComponent.Storage storage = accessor.getComponent(
        ref,
        InventoryComponent.Storage.getComponentType());
    return storage == null ? null : storage.getInventory();
  }

  public static InventoryComponent.Hotbar hotbar(
      ComponentAccessor<EntityStore> accessor,
      Ref<EntityStore> ref) {
    return accessor.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
  }

  public static ItemContainer hotbarContainer(
      ComponentAccessor<EntityStore> accessor,
      Ref<EntityStore> ref) {
    InventoryComponent.Hotbar hotbar = hotbar(accessor, ref);
    return hotbar == null ? null : hotbar.getInventory();
  }

  public static byte activeHotbarSlot(
      ComponentAccessor<EntityStore> accessor,
      Ref<EntityStore> ref) {
    InventoryComponent.Hotbar hotbar = hotbar(accessor, ref);
    return hotbar == null ? InventoryComponent.INACTIVE_SLOT_INDEX : hotbar.getActiveSlot();
  }

  public static ItemStack activeHotbarItem(
      ComponentAccessor<EntityStore> accessor,
      Ref<EntityStore> ref) {
    InventoryComponent.Hotbar hotbar = hotbar(accessor, ref);
    return hotbar == null ? null : hotbar.getActiveItem();
  }

  public static ItemStack heldItem(
      ComponentAccessor<EntityStore> accessor,
      Ref<EntityStore> ref) {
    return InventoryComponent.getItemInHand(accessor, ref);
  }

  public static CombinedItemContainer combinedStorageFirst(
      ComponentAccessor<EntityStore> accessor,
      Ref<EntityStore> ref) {
    return InventoryComponent.getCombined(accessor, ref, InventoryComponent.STORAGE_FIRST);
  }
}
