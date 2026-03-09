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
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderEquipItemFromInventory;

public class EquipItemFromInventory extends ActionBase {
  private String itemIdToEquip;

  public EquipItemFromInventory(BuilderEquipItemFromInventory builderActionBase, BuilderSupport support) {
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

    ItemContainer inventory = npcEntity.getInventory().getCombinedEverything();
    byte activeHotbarSlot = npcEntity
        .getInventory()
        .getActiveHotbarSlot();

    for (short slot = 0; slot < inventory.getCapacity(); slot++) {
      ItemStack itemStack = inventory.getItemStack(slot);
      if (itemStack != null && itemStack.getItemId().startsWith(this.itemIdToEquip)) {
        ItemContainer hotbar = npcEntity.getInventory().getHotbar();

        ItemStack equippedStack = hotbar.getItemStack(
            activeHotbarSlot);

        hotbar.setItemStackForSlot(
            activeHotbarSlot,
            itemStack);

        inventory.setItemStackForSlot(slot, equippedStack);
        return true;
      }
    }

    return false;
  }
}
