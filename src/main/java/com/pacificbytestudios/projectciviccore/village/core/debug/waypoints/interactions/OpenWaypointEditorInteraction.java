package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.interactions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3i;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.WaypointsSessionUtil;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.Waypoint;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointOperation;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointsEditingComponent;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.ui.WaypointEditorPage;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.ui.WaypointSessionPage;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;

@SerializableClass
public class OpenWaypointEditorInteraction extends SimpleBlockInteraction {
  public static final BuilderCodec<OpenWaypointEditorInteraction> CODEC = OpenWaypointEditorInteractionCodec.CODEC;

  double precisionThreshold;

  @Override
  @Nonnull
  public WaitForDataFrom getWaitForDataFrom() {
    return WaitForDataFrom.Server;
  }

  @Override
  protected void interactWithBlock(
      @Nonnull World world,
      @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull InteractionType type,
      @Nonnull InteractionContext context,
      @Nullable ItemStack itemInHand,
      @Nonnull Vector3i targetBlock,
      @Nonnull CooldownHandler cooldownHandler) {
    Ref<EntityStore> ref = context.getOwningEntity();
    WaypointsEditingComponent editingComponent = commandBuffer.getComponent(ref,
        WaypointsEditingComponent.getComponentType());

    Player player = commandBuffer.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
    if (player == null || playerRef == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    if (editingComponent == null) {
      playerRef.sendMessage(Message.raw(
          "Select a structure first by left clicking it with the waypoint tool"));
      context.getState().state = InteractionState.Failed;
      return;
    }

    // INFO: Crouched use reviews the session (save / rollback) instead of editing.
    if (isCrouching(commandBuffer, ref)) {
      openSessionPage(world, commandBuffer, ref, player, playerRef, editingComponent, context);
      return;
    }

    Vec3d hit = Vec3d.from(TargetUtil.getTargetLocation(context.getEntity(), this.viewDistance, commandBuffer));

    if (hit == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    Waypoint waypointFound = editingComponent.getNearestWaypoint(hit, this.precisionThreshold);

    if (waypointFound != null) {
      WaypointOperation currentOperation = editingComponent.getCurrentOperation();
      if (currentOperation != null && currentOperation.getNewWaypoint() != null) {
        editingComponent.addOperation(currentOperation);
      }
      editingComponent.setCurrentOperation(new WaypointOperation(
          null, waypointFound, null));
    }

    player.getPageManager().openCustomPage(
        ref,
        commandBuffer.getStore(),
        new WaypointEditorPage(playerRef, world, editingComponent, waypointFound, hit));
  }

  private void openSessionPage(
      World world,
      CommandBuffer<EntityStore> commandBuffer,
      Ref<EntityStore> ref,
      Player player,
      PlayerRef playerRef,
      WaypointsEditingComponent editingComponent,
      InteractionContext context) {
    editingComponent.commitCurrentOperation();

    WaypointsSessionUtil.SessionContext sessionContext = WaypointsSessionUtil.resolveContext(editingComponent);
    if (sessionContext == null) {
      playerRef.sendMessage(Message.raw("Could not resolve the structure of this editing session"));
      context.getState().state = InteractionState.Failed;
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

  private static boolean isCrouching(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref) {
    MovementStatesComponent movement = commandBuffer.getComponent(ref,
        MovementStatesComponent.getComponentType());
    return movement != null
        && movement.getMovementStates() != null
        && movement.getMovementStates().crouching;
  }

  @Override
  protected void simulateInteractWithBlock(
      InteractionType interactionType,
      InteractionContext context,
      ItemStack itemStack,
      World world,
      Vector3i block) {
    throw new UnsupportedOperationException("Unimplemented method 'simulateInteractWithBlock'");
  }

}
