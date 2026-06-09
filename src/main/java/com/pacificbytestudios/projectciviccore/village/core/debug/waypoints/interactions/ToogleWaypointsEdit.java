package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.interactions;

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
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.Interactable;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.DrawUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.VillageBedComponent;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.WaypointsDrawer;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointsEditingComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
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

    if (editingComponent != null &&
        structure.getStructureId().equals(editingComponent.getStructureId())) {
      DrawUtils.clear(world);
      // TODO: ask to save or rollback
      commandBuffer.tryRemoveComponent(ref, WaypointsEditingComponent.getComponentType());
      return;
    }

    editingComponent = new WaypointsEditingComponent();
    editingComponent.setStructureId(structure.getStructureId());
    populateWaypoints(editingComponent, village, structure);

    DrawUtils.clear(world);
    WaypointsDrawer.draw(world, editingComponent.getStructureWaypoints());
    commandBuffer.putComponent(ref, WaypointsEditingComponent.getComponentType(), editingComponent);
  }

  private static void populateWaypoints(
      WaypointsEditingComponent editingComponent,
      VillageHeartComponent village,
      TopologyStructure structure) {
    StructureType structureType = structure.getStructureType();
    if (structureType == StructureType.HOUSE) {
      populateHouseWaypoints(editingComponent, village.getHouseFromId(structure.getStructureId()));
      return;
    }
    populateWorkplaceWaypoints(editingComponent, village.getWorkplaceFromId(structure.getStructureId()));
  }

  private static void populateWorkplaceWaypoints(
      WaypointsEditingComponent editingComponent,
      WorkplaceComponent workplace) {
    BlockTransform<Vec3i> bench = workplace.getBenchTransform();
    editingComponent.addStructureWaypoint(MarkerType.BENCH, BlockTransform.clone(bench));
    editingComponent.addStructureWaypoint(MarkerType.BENCH_ACCESS,
        accessWaypoint(bench, workplace.getBenchAccessPos()));
    editingComponent.addStructureWaypoint(MarkerType.ANCHOR,
        BlockTransform.clone(workplace.getPrefabAnchorTransform()));

    workplace.getStorage().getContainers().values().forEach(container -> {
      BlockTransform<Vec3i> containerTransform = container.getContainerTransform();
      editingComponent.addStructureWaypoint(MarkerType.STORAGE, BlockTransform.clone(containerTransform));
      editingComponent.addStructureWaypoint(MarkerType.STORAGE_ACCESS, accessWaypoint(containerTransform,
          container.getAccessPosition()));
    });

    WorkplaceInteractablesMapComponent interactables = workplace.getComponent(
        WorkplaceInteractablesMapComponent.getComponentType());
    if (interactables != null) {
      populateInteractableWaypoints(editingComponent, interactables);
    }
  }

  private static void populateInteractableWaypoints(
      WaypointsEditingComponent editingComponent,
      WorkplaceInteractablesMapComponent workplaceInteractables) {
    workplaceInteractables.getAllInteractables().forEach((type, interactables) -> {
      for (Interactable interactable : interactables) {
        editingComponent.addStructureWaypoint(type.getMarkerType(), waypoint(interactable.getInteractableTransform()));

        if (type.getDependsOn() == MarkerType.NONE) {
          continue;
        }

        editingComponent.addStructureWaypoint(type.getDependsOn(), waypoint(interactable.getDependsOnTransform()));
      }
    });
  }

  private static void populateHouseWaypoints(
      WaypointsEditingComponent editingComponent,
      HouseComponent house) {
    editingComponent.addStructureWaypoint(MarkerType.BENCH, new BlockTransform<>(house.getCoreBlockPosition()));
    editingComponent.addStructureWaypoint(MarkerType.ANCHOR, new BlockTransform<>(house.getPrefabAnchor()));

    house.getAppartmentRegistry().getAppartments().values().forEach(appartment -> {
      for (VillageBedComponent bed : appartment.getBeds().values()) {
        editingComponent.addStructureWaypoint(MarkerType.BED, BlockTransform.clone(bed.getBedTransform()));
      }
    });
  }

  private static BlockTransform<Vec3i> accessWaypoint(BlockTransform<Vec3i> source, Vec3f accessPosition) {
    BlockTransform<Vec3i> waypoint = BlockTransform.clone(source);
    waypoint.setAccessOffset(Vec3f.from(accessPosition));
    return waypoint;
  }

  private static BlockTransform<Vec3i> waypoint(BlockTransform<Vec3f> source) {
    BlockTransform<Vec3i> waypoint = new BlockTransform<>();
    waypoint.setPosition(Vec3i.from(source.getPosition()));
    waypoint.setPositionId(source.getPositionId());
    waypoint.setRotation(source.getRotation());
    waypoint.setAccessId(source.getAccessId());
    waypoint.setAccessOffset(Vec3f.from(source.getAccessOffset()));
    return waypoint;
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
