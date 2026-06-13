package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.interactions;

import java.util.UUID;

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
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.WaypointsSessionUtil;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.Waypoint;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointOperation;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointsEditingComponent;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.ui.WaypointSessionPage;

@SerializableClass
public class ToggleWaypointsEdit extends SimpleBlockInteraction {
  public static final BuilderCodec<ToggleWaypointsEdit> CODEC = ToggleWaypointsEditCodec.CODEC;

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

    Vec3d hit = Vec3d.from(TargetUtil.getTargetLocation(context.getEntity(), this.viewDistance, commandBuffer));

    if (hit == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    VillageHeartComponent village = WaypointsSessionUtil.getVillageAt(hit);
    if (village == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    if (!village.getConfig().isDebugActive()) {
      sendMessage(commandBuffer, ref,
          "Village debug is not active");
      context.getState().state = InteractionState.Failed;
      return;
    }

    VillageTopologyComponent topology = village.getComponent(
        VillageTopologyComponent.getComponentType());
    if (topology == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    TopologyStructure structure = topology.getTopologyStructureAt(hit.to3i());
    if (structure == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    // INFO: If not in an editing session
    if (editingComponent == null) {
      if (!handleNewEditingSession(world, structure, village, commandBuffer, ref)) {
        context.getState().state = InteractionState.Failed;
      }
      return;
    }

    // INFO: If already in an editing session, but changed the structure
    if (!structure.getStructureId().equals(editingComponent.getStructureId())) {
      handleStructureSwitch(world, commandBuffer, ref, context, editingComponent, village, structure);
      return;
    }

    // INFO: If already in an editing session, but same structure
    WaypointOperation currentOperation = editingComponent.getCurrentOperation();
    if (currentOperation == null || currentOperation.getOldWaypoint() == null) {
      sendMessage(commandBuffer, ref,
          "Select a waypoint with Use and press Move first");
      context.getState().state = InteractionState.Failed;
      return;
    }

    handleContinueEditingSession(world, hit, editingComponent);
  }

  private boolean handleNewEditingSession(
      World world,
      TopologyStructure structure,
      VillageHeartComponent village,
      CommandBuffer<EntityStore> commandBuffer,
      Ref<EntityStore> ref) {
    WaypointsEditingComponent editingComponent = new WaypointsEditingComponent();
    editingComponent.setStructureId(structure.getStructureId());
    if (!WaypointsSessionUtil.populateWaypoints(editingComponent, village, structure)) {
      return false;
    }
    WaypointsSessionUtil.redraw(world, editingComponent);
    commandBuffer.putComponent(ref, WaypointsEditingComponent.getComponentType(), editingComponent);
    return true;
  }

  /**
   * The player clicked a different structure while a session is active. With no
   * unsaved
   * changes the session switches immediately; otherwise the session page asks to
   * save,
   * roll back (both switch afterwards) or cancel (keeps the current session).
   */
  private void handleStructureSwitch(
      World world,
      CommandBuffer<EntityStore> commandBuffer,
      Ref<EntityStore> ref,
      InteractionContext context,
      WaypointsEditingComponent editingComponent,
      VillageHeartComponent village,
      TopologyStructure structure) {
    editingComponent.commitCurrentOperation();

    if (!editingComponent.hasPendingChanges()) {
      if (!handleNewEditingSession(world, structure, village, commandBuffer, ref)) {
        context.getState().state = InteractionState.Failed;
      }
      return;
    }

    Player player = commandBuffer.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
    if (player == null || playerRef == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    WaypointsSessionUtil.SessionContext current = WaypointsSessionUtil.resolveContext(editingComponent);
    if (current == null) {
      playerRef.sendMessage(Message.raw(
          "Unsaved changes, but the current structure could not be resolved; use Shift+Use to review them"));
      context.getState().state = InteractionState.Failed;
      return;
    }

    player.getPageManager().openCustomPage(
        ref,
        commandBuffer.getStore(),
        new WaypointSessionPage(
            playerRef,
            world,
            current.village(),
            current.structure(),
            editingComponent,
            new WaypointSessionPage.SwitchTarget(village, structure)));
  }

  private void handleContinueEditingSession(
      World world,
      Vec3d target,
      WaypointsEditingComponent editingComponent) {
    WaypointOperation currentOperation = editingComponent.getCurrentOperation();
    Waypoint oldWaypoint = currentOperation.getOldWaypoint();

    Waypoint newWaypoint = currentOperation.getNewWaypoint();
    newWaypoint = newWaypoint == null ? oldWaypoint.clone() : newWaypoint;

    UUID changedWaypointId = null;
    if (newWaypoint.isAccess()) {
      newWaypoint.setAccessPosition(target);
      changedWaypointId = newWaypoint.getAccessId();
    } else {
      newWaypoint.setPosition(target);
      changedWaypointId = newWaypoint.getPositionId();
    }

    currentOperation.setNewWaypoint(newWaypoint);
    editingComponent.updateStructureWaypoint(
        newWaypoint.getType(),
        changedWaypointId,
        newWaypoint);

    WaypointsSessionUtil.redraw(world, editingComponent);
  }

  private static void sendMessage(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> ref, String text) {
    PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
    if (playerRef != null) {
      playerRef.sendMessage(Message.raw(text));
    }
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
