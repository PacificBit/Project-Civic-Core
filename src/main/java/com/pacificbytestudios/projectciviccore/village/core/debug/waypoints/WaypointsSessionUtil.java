package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints;

import java.util.Map.Entry;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
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
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.Waypoint;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.WaypointsEditingComponent;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

public final class WaypointsSessionUtil {

  public record SessionContext(VillageHeartComponent village, TopologyStructure structure) {
  }

  private WaypointsSessionUtil() {
  }

  /**
   * Resolves the village and topology structure of an active editing session from the
   * world position of any of its waypoints.
   */
  public static SessionContext resolveContext(WaypointsEditingComponent editingComponent) {
    if (editingComponent == null || editingComponent.getStructureId() == null) {
      return null;
    }

    Vec3d anyPosition = anyWaypointPosition(editingComponent);
    if (anyPosition == null) {
      return null;
    }

    VillageHeartComponent village = getVillageAt(anyPosition);
    if (village == null) {
      return null;
    }

    VillageTopologyComponent topology = village.getComponent(
        VillageTopologyComponent.getComponentType());
    if (topology == null) {
      return null;
    }

    TopologyStructure structure = topology.getTopologyStructureFromStructureId(
        editingComponent.getStructureId());
    if (structure == null) {
      return null;
    }

    return new SessionContext(village, structure);
  }

  public static VillageHeartComponent getVillageAt(Vec3d position) {
    EntityRef heartRef = VillageHeartRegistry.get().getVillageHeartIn(position.to2i());
    if (heartRef == null || heartRef.getRef() == null) {
      return null;
    }

    Store<ChunkStore> chunkStore = heartRef.getRef().getStore();
    return chunkStore.getComponent(heartRef.getRef(),
        VillageHeartComponent.getComponentType());
  }

  public static boolean populateWaypoints(
      WaypointsEditingComponent editingComponent,
      VillageHeartComponent village,
      TopologyStructure structure) {
    StructureType structureType = structure.getStructureType();
    if (structureType == StructureType.HOUSE) {
      HouseComponent house = village.getHouseFromId(structure.getStructureId());
      if (house == null) {
        return false;
      }
      populateHouseWaypoints(editingComponent, house);
      return true;
    }

    WorkplaceComponent workplace = village.getWorkplaceFromId(structure.getStructureId());
    if (workplace == null) {
      return false;
    }
    populateWorkplaceWaypoints(editingComponent, workplace);
    return true;
  }

  public static void redraw(World world, WaypointsEditingComponent editingComponent) {
    DrawUtils.clear(world);
    if (editingComponent != null) {
      WaypointsDrawer.draw(world, editingComponent.getStructureWaypoints());
    }
  }

  private static void populateWorkplaceWaypoints(
      WaypointsEditingComponent editingComponent,
      WorkplaceComponent workplace) {
    BlockTransform<Vec3i> bench = workplace.getBenchTransform();
    bench.ensurePositionId();
    editingComponent.addStructureWaypoint(MarkerType.BENCH, BlockTransform.clone(bench));
    editingComponent.addStructureWaypoint(MarkerType.BENCH_ACCESS,
        accessWaypoint(bench, workplace.getBenchAccessPos()));
    editingComponent.addStructureWaypoint(MarkerType.ANCHOR,
        BlockTransform.clone(workplace.getPrefabAnchorTransform()));

    workplace.getStorage().getContainers().values().forEach(container -> {
      BlockTransform<Vec3i> containerTransform = container.getContainerTransform();
      containerTransform.ensurePositionId();
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
        editingComponent.addStructureWaypoint(type.getMarkerType(), interactable.getInteractableTransform());

        if (type.getDependsOn() == MarkerType.NONE) {
          continue;
        }

        editingComponent.addStructureWaypoint(type.getDependsOn(), interactable.getDependsOnTransform());
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

  private static Vec3d anyWaypointPosition(WaypointsEditingComponent editingComponent) {
    for (Entry<MarkerType, Waypoint[]> entry : editingComponent.getStructureWaypoints().entrySet()) {
      for (Waypoint waypoint : entry.getValue()) {
        if (waypoint != null) {
          return waypoint.getWaypointPosition();
        }
      }
    }

    return null;
  }
}
