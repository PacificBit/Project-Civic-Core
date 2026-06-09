package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.interactions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3i;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointsEditingComponent;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointsEditingComponent.Waypoint;

public class OpenWaypointEditorInteraction extends SimpleBlockInteraction {
  public static final BuilderCodec<OpenWaypointEditorInteraction> CODEC = BuilderCodec
      .builder(OpenWaypointEditorInteraction.class, OpenWaypointEditorInteraction::new, SimpleBlockInteraction.CODEC)

      .append(new KeyedCodec<>("PrecisionThreshold", Codec.DOUBLE),
          (obj, value) -> obj.precisionThreshold = value,
          (obj) -> obj.precisionThreshold)
      .add()

      .build();

  private double precisionThreshold;

  @SuppressWarnings("unchecked")
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
    if (editingComponent == null) {
      // TODO: send a message, "A structure needs to be selected first"
      return;
    }

    Vec3d hit = Vec3d.from(TargetUtil.getTargetLocation(context.getEntity(), this.viewDistance, commandBuffer));

    if (hit == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    Waypoint waypointFound = editingComponent.getNearestWaypoint(hit, this.precisionThreshold);
    if (waypointFound == null) {
      Logger.logSevere("Nothing hit");
      return;
    }

    BlockTransform<Vec3i> transform = (BlockTransform<Vec3i>) waypointFound.transform();
    Logger.logInfo("Hitted waypoint at: %s",
        WaypointsEditingComponent.waypointPosition(transform, waypointFound.isAccess()));
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
