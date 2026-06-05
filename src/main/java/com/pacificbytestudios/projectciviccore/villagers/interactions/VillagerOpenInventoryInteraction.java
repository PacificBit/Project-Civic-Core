package com.pacificbytestudios.projectciviccore.villagers.interactions;

import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerWindow;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.VillagerInventoryAccess;

public class VillagerOpenInventoryInteraction extends SimpleInstantInteraction {
  public static final BuilderCodec<VillagerOpenInventoryInteraction> CODEC = BuilderCodec
      .builder(
          VillagerOpenInventoryInteraction.class,
          VillagerOpenInventoryInteraction::new,
          SimpleInstantInteraction.CODEC)
      .build();

  @Override
  protected void firstRun(
      InteractionType interactionType,
      InteractionContext context,
      CooldownHandler cooldown) {
    Ref<EntityStore> playerRef = context.getEntity();
    CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();

    Player player = commandBuffer.getComponent(playerRef, Player.getComponentType());
    if (player == null) {
      Logger.logWarning("Could not find player using interaction context");
      return;
    }

    UUIDComponent uuidComp = commandBuffer.getComponent(playerRef, UUIDComponent.getComponentType());
    if (uuidComp == null) {
      Logger.logWarning("Could not find UUIDComponent for player");
      return;
    }
    UUID playerUuid = uuidComp.getUuid();

    Store<EntityStore> store = playerRef.getStore();

    Ref<EntityStore> target = context.getTargetEntity();
    if (target == null) {
      Logger.logWarning("Target entity is null. playerUuid=%s".formatted(playerUuid));
      return;
    }

    if (!target.isValid()) {
      Logger.logWarning("Target entity is invalid. playerUuid=%s, target=%s"
          .formatted(playerUuid, target));
      return;
    }

    VillagerComponent villager = store.getComponent(target, VillagerComponent.getComponentType());
    if (villager == null) {
      Logger.logWarning("Target entity is missing VillagerComponent. playerUuid=%s, target=%s"
          .formatted(playerUuid, target));
      return;
    }

    ItemContainer container = VillagerInventoryAccess.combinedStorageFirst(commandBuffer, target);
    if (container == null) {
      Logger.logWarning("Combined storage container is null after inventory setup. playerUuid=%s, target=%s"
          .formatted(playerUuid, target));
      return;
    }

    Map<UUID, ContainerWindow> windows = villager.getWindows();
    ContainerWindow window = new ContainerWindow(container);

    if (windows.putIfAbsent(playerUuid, window) != null) {
      Logger.logWarning("Player already has an open villager inventory window. playerUuid=%s, target=%s"
          .formatted(playerUuid, target));
      return;
    }

    if (!player.getPageManager().setPageWithWindows(playerRef, store, Page.Bench, true, window)) {
      Logger.logWarning("Failed to open villager inventory page for player. playerUuid=%s, target=%s"
          .formatted(playerUuid, target));
      windows.remove(playerUuid, window);
      return;
    }

    window.registerCloseEvent(_ -> windows.remove(playerUuid, window));
  }
}
