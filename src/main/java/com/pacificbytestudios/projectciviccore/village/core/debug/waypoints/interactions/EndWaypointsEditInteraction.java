package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.WaypointsSessionUtil;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointsEditingComponent;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.ui.WaypointExitConfirmPage;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.ui.WaypointSessionPage;

/**
 * Ends the active waypoint editing session. With pending changes it opens the review page
 * (which offers Save &amp; Exit); with no changes it opens a small confirmation popup.
 */
@SerializableClass
public class EndWaypointsEditInteraction extends SimpleInstantInteraction {
  public static final BuilderCodec<EndWaypointsEditInteraction> CODEC = EndWaypointsEditInteractionCodec.CODEC;

  @Override
  protected void firstRun(
      InteractionType type,
      InteractionContext context,
      CooldownHandler cooldownHandler) {
    Ref<EntityStore> ref = context.getEntity();
    CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();

    Player player = commandBuffer.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
    if (player == null || playerRef == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    WaypointsEditingComponent editingComponent = commandBuffer.getComponent(ref,
        WaypointsEditingComponent.getComponentType());
    if (editingComponent == null) {
      playerRef.sendMessage(Message.raw("No active waypoint editing session"));
      context.getState().state = InteractionState.Failed;
      return;
    }

    World world = Universe.get().getWorld(playerRef.getWorldUuid());
    if (world == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    editingComponent.commitCurrentOperation();

    if (!editingComponent.hasPendingChanges()) {
      player.getPageManager().openCustomPage(
          ref,
          commandBuffer.getStore(),
          new WaypointExitConfirmPage(playerRef, world));
      return;
    }

    WaypointsSessionUtil.SessionContext sessionContext = WaypointsSessionUtil.resolveContext(editingComponent);
    if (sessionContext == null) {
      // Nothing resolvable to review; just tear the session down.
      WaypointsSessionUtil.exitSession(world, commandBuffer.getStore(), ref, editingComponent);
      playerRef.sendMessage(Message.raw("Waypoint editing session ended"));
      return;
    }

    player.getPageManager().openCustomPage(
        ref,
        commandBuffer.getStore(),
        new WaypointSessionPage(
            playerRef,
            world,
            sessionContext.village(),
            sessionContext.structure(),
            editingComponent,
            null));
  }
}
