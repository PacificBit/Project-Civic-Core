package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.interactions;

import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3i;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.utils.DrawUtils;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.WaypointDrawer;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointsEditingComponent;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

public class ToogleWaypointsEdit extends SimpleBlockInteraction {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  public static final BuilderCodec<ToogleWaypointsEdit> CODEC = BuilderCodec
      .builder(ToogleWaypointsEdit.class, ToogleWaypointsEdit::new, SimpleBlockInteraction.CODEC)
      .build();

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

    if (editingComponent != null) {
      DrawUtils.clear(world);
      // TODO: ask to save or rollback
      return;
    }

    Vec3d hit = Vec3d.from(TargetUtil.getTargetLocation(context.getEntity(), this.viewDistance, commandBuffer));

    if (hit == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    VillageHeartComponent village = getVillage(hit);
    if (village == null) {
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

    WorkplaceComponent workplace = village.getWorkplaceFromId(structure.getStructureId());

    editingComponent = new WaypointsEditingComponent();
    editingComponent.setStructureId(structure.getStructureId());

    DrawUtils.clear(world);
    WaypointDrawer.draw(world, workplace, new ArrayList<>(editingComponent.getDirtyWaypoints()));
    commandBuffer.putComponent(ref, WaypointsEditingComponent.getComponentType(), editingComponent);
  }

  private VillageHeartComponent getVillage(Vec3d hit) {
    EntityRef heartRef = villageStore.getVillageHeartIn(hit.to2i());
    if (heartRef == null || heartRef.getRef() == null) {
      return null;
    }

    Store<ChunkStore> chunkStore = heartRef.getRef().getStore();
    return chunkStore.getComponent(heartRef.getRef(),
        VillageHeartComponent.getComponentType());
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
