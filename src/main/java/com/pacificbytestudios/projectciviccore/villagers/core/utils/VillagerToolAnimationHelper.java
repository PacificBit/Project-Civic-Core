package com.pacificbytestudios.projectciviccore.villagers.core.utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.ActiveAnimationComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.VillagerInventoryAccess;

public final class VillagerToolAnimationHelper {
  private VillagerToolAnimationHelper() {
  }

  public static void playHeldToolAnimation(
      Ref<EntityStore> ref,
      NPCEntity npc,
      Store<EntityStore> store,
      String animationId) {
    if (ref == null || npc == null || store == null || animationId == null || animationId.isBlank()) {
      return;
    }

    ItemStack itemStack = VillagerInventoryAccess.heldItem(store, ref);
    if (itemStack == null) {
      return;
    }

    Item item = itemStack.getItem();
    if (item == null) {
      return;
    }

    String playerAnimationsId = item.getPlayerAnimationsId();
    if (playerAnimationsId == null || playerAnimationsId.isBlank()) {
      return;
    }

    ActiveAnimationComponent activeAnimationComponent = store.getComponent(ref,
        ActiveAnimationComponent.getComponentType());
    if (activeAnimationComponent != null) {
      activeAnimationComponent.getActiveAnimations()[AnimationSlot.Action.ordinal()] = animationId;
      activeAnimationComponent.setPlayingAnimation(AnimationSlot.Action, animationId);
    }

    AnimationUtils.playAnimation(ref, AnimationSlot.Action, playerAnimationsId, animationId, store);
  }
}
