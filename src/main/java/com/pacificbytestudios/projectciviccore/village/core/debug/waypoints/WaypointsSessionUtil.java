package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform3D;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.Interactable;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.StructureLevelConfig;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.DrawUtils;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartDebugComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.VillageBedComponent;
import com.pacificbytestudios.projectciviccore.village.components.structures.VillageStructuresConfig;
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
      applyConfigRotations(editingComponent, village, structure);
      return true;
    }

    WorkplaceComponent workplace = village.getWorkplaceFromId(structure.getStructureId());
    if (workplace == null) {
      return false;
    }
    populateWorkplaceWaypoints(editingComponent, workplace);
    applyConfigRotations(editingComponent, village, structure);
    return true;
  }

  /**
   * Spawners do not store block rotations on the live components, so the session derives
   * each block waypoint's world rotation from the structure config (config rotation plus
   * the structure orientation, the same math used when the block was placed).
   */
  private static void applyConfigRotations(
      WaypointsEditingComponent editingComponent,
      VillageHeartComponent village,
      TopologyStructure structure) {
    VillageStructuresConfig structuresConfig = village.getComponent(
        VillageStructuresConfig.getComponetType());
    if (structuresConfig == null) {
      return;
    }

    StructureLevelConfig levelConfig = structuresConfig.getStructureLevelConfigsFor(
        structure.getStructureType());
    Short level = resolveLevel(village, structure);
    if (levelConfig == null || level == null) {
      return;
    }

    Rotation orientation = StructurePlacementUtils.normalize(structure.getOrientation());

    BlockTransform3D coreBlock = levelConfig.getCoreBlockFor(level);
    if (coreBlock != null && coreBlock.getRotation() != null) {
      applyRotation(
          firstWaypointOf(editingComponent, MarkerType.BENCH),
          StructurePlacementUtils.addYaw(coreBlock.getRotation(), orientation));
    }

    for (BlockTransform3D container : levelConfig.getContainersFor(level)) {
      if (container == null || container.getRotation() == null) {
        continue;
      }

      Vec3i worldPosition = StructurePlacementUtils.withOffset(
          structure.getPrefabAnchor(),
          container.getPosition3i(),
          orientation);
      applyRotation(
          waypointAt(editingComponent, MarkerType.STORAGE, worldPosition),
          StructurePlacementUtils.addYaw(container.getRotation(), orientation));
    }
  }

  private static void applyRotation(Waypoint waypoint, Rotation worldRotation) {
    if (waypoint == null || waypoint.getTransform() == null) {
      return;
    }
    if (waypoint.getTransform().getRotation() == null) {
      waypoint.getTransform().setRotation(worldRotation);
    }
  }

  private static Waypoint firstWaypointOf(WaypointsEditingComponent editingComponent, MarkerType type) {
    Waypoint[] waypoints = editingComponent.getStructureWaypoints().get(type);
    if (waypoints == null) {
      return null;
    }

    for (Waypoint waypoint : waypoints) {
      if (waypoint != null) {
        return waypoint;
      }
    }
    return null;
  }

  private static Waypoint waypointAt(
      WaypointsEditingComponent editingComponent,
      MarkerType type,
      Vec3i blockPosition) {
    Waypoint[] waypoints = editingComponent.getStructureWaypoints().get(type);
    if (waypoints == null) {
      return null;
    }

    for (Waypoint waypoint : waypoints) {
      if (waypoint != null && waypoint.getWaypointPosition().to3i().equals(blockPosition)) {
        return waypoint;
      }
    }
    return null;
  }

  private static Short resolveLevel(VillageHeartComponent village, TopologyStructure structure) {
    if (structure.getStructureType() == StructureType.HOUSE) {
      HouseComponent house = village.getHouseFromId(structure.getStructureId());
      return house == null ? null : house.getLevel();
    }

    WorkplaceComponent workplace = village.getWorkplaceFromId(structure.getStructureId());
    return workplace == null ? null : workplace.getLevel();
  }

  public static void redraw(World world, WaypointsEditingComponent editingComponent) {
    if (editingComponent == null) {
      DrawUtils.clear(world);
      return;
    }

    SessionContext context = resolveContext(editingComponent);
    if (context == null) {
      DrawUtils.clear(world);
      return;
    }

    VillageHeartDebugComponent debug = context.village().getComponent(
        VillageHeartDebugComponent.getComponentType());
    if (debug == null) {
      DrawUtils.clear(world);
      return;
    }

    debug.setEditingSession(
        editingComponent.getStructureId(),
        debug.getWaypointEditorPlayerId(),
        toMarkerListMap(editingComponent.getStructureWaypoints()));
    debug.drawAll(world);
  }

  /**
   * Ends an editing session: removes the editing component from the player, drops the live
   * session markers from the debug component, and refreshes the world view. When the debug
   * command is also showing structure markers, the edited structure's entry is updated with
   * the session's final waypoints and everything is redrawn; otherwise the world is cleared.
   */
  public static void exitSession(
      World world,
      Store<EntityStore> store,
      Ref<EntityStore> playerRef,
      WaypointsEditingComponent editingComponent) {
    if (editingComponent != null) {
      editingComponent.commitCurrentOperation();

      SessionContext context = resolveContext(editingComponent);
      VillageHeartDebugComponent debug = context == null
          ? null
          : context.village().getComponent(VillageHeartDebugComponent.getComponentType());

      if (debug != null) {
        debug.clearEditingSession();
        if (debug.hasStructureMarkers()) {
          debug.setStructureWaypoints(
              editingComponent.getStructureId(),
              toMarkerListMap(editingComponent.getStructureWaypoints()));
          debug.drawAll(world);
        } else {
          DrawUtils.clear(world);
        }
      } else {
        DrawUtils.clear(world);
      }
    } else {
      DrawUtils.clear(world);
    }

    store.tryRemoveComponent(playerRef, WaypointsEditingComponent.getComponentType());
  }

  /** Converts the editing component's array-backed marker map into the list form the debug
   * component stores, dropping any null slots. */
  public static Map<MarkerType, List<Waypoint>> toMarkerListMap(Map<MarkerType, Waypoint[]> source) {
    Map<MarkerType, List<Waypoint>> result = new HashMap<>();
    for (Entry<MarkerType, Waypoint[]> entry : source.entrySet()) {
      List<Waypoint> waypoints = new ArrayList<>();
      for (Waypoint waypoint : entry.getValue()) {
        if (waypoint != null) {
          waypoints.add(waypoint);
        }
      }
      result.put(entry.getKey(), waypoints);
    }
    return result;
  }

  private static void populateWorkplaceWaypoints(
      WaypointsEditingComponent editingComponent,
      WorkplaceComponent workplace) {
    BlockTransform3D bench = workplace.getBenchTransform();
    bench.ensurePositionId();
    editingComponent.addStructureWaypoint(MarkerType.BENCH, BlockTransform3D.clone(bench));
    editingComponent.addStructureWaypoint(MarkerType.BENCH_ACCESS,
        accessWaypoint(bench, workplace.getBenchAccessPos()));
    editingComponent.addStructureWaypoint(MarkerType.ANCHOR,
        BlockTransform3D.clone(workplace.getPrefabAnchorTransform()));

    workplace.getStorage().getContainers().values().forEach(container -> {
      BlockTransform3D containerTransform = container.getContainerTransform();
      containerTransform.ensurePositionId();
      editingComponent.addStructureWaypoint(MarkerType.STORAGE, BlockTransform3D.clone(containerTransform));
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
    editingComponent.addStructureWaypoint(MarkerType.BENCH, new BlockTransform3D(house.getCoreBlockPosition()));
    editingComponent.addStructureWaypoint(MarkerType.ANCHOR, new BlockTransform3D(house.getPrefabAnchor()));

    house.getAppartmentRegistry().getAppartments().values().forEach(appartment -> {
      for (VillageBedComponent bed : appartment.getBeds().values()) {
        editingComponent.addStructureWaypoint(MarkerType.BED, BlockTransform3D.clone(bed.getBedTransform()));
      }
    });
  }

  private static BlockTransform3D accessWaypoint(BlockTransform3D source, Vec3f accessPosition) {
    BlockTransform3D waypoint = BlockTransform3D.clone(source);
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
