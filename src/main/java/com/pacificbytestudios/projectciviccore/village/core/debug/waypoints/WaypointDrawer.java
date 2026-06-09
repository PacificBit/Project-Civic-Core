package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.joml.Matrix4d;

import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.Interactable;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.DrawUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerRegistryComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.village.enums.InteractablesType;

@Deprecated
public class WaypointDrawer {
  private static final double MARKER_SCALE = 0.2;

  public static void draw(World world, WorkplaceComponent workplace, List<UUID> blacklist) {
    Map<MarkerType, List<Vec3d>> markers = new EnumMap<>(MarkerType.class);
    addWorkplaceMarkers(workplace, markers, toBlacklistSet(blacklist));
    drawMarkers(world, markers);
  }

  private static Set<UUID> toBlacklistSet(List<UUID> blacklist) {
    return new HashSet<>(blacklist);
  }

  private static void addWorkplaceMarkers(
      WorkplaceComponent workplace,
      Map<MarkerType, List<Vec3d>> markers,
      Set<UUID> blacklist) {
    addTransformMarker(markers, MarkerType.BENCH, workplace.getBenchTransform(), blacklist);
    addAccessOffsetMarker(markers, MarkerType.BENCH_ACCESS, workplace.getBenchTransform(), blacklist);
    addTransformMarker(markers, MarkerType.ANCHOR, workplace.getPrefabAnchorTransform(), blacklist);

    WorkplaceContainerRegistryComponent storage = workplace.getStorage();
    storage.getContainers().values().forEach(container -> {
      addTransformMarker(markers, MarkerType.STORAGE, container.getContainerTransform(), blacklist);
      addAccessOffsetMarker(markers, MarkerType.STORAGE_ACCESS, container.getContainerTransform(), blacklist);
    });

    WorkplaceInteractablesMapComponent interactables = workplace.getComponent(
        WorkplaceInteractablesMapComponent.getComponentType());
    if (interactables != null) {
      addInteractables(interactables, markers, blacklist);
    }
  }

  private static void addInteractables(
      WorkplaceInteractablesMapComponent workplaceInteractables,
      Map<MarkerType, List<Vec3d>> markers,
      Set<UUID> blacklist) {
    Map<InteractablesType, Interactable[]> interactableMap = workplaceInteractables.getAllInteractables();

    for (Entry<InteractablesType, Interactable[]> interactableEntry : interactableMap.entrySet()) {
      InteractablesType key = interactableEntry.getKey();
      Interactable[] interactables = interactableEntry.getValue();

      for (Interactable interactable : interactables) {
        addTransformMarker(markers, key.getMarkerType(), interactable.getInteractableTransform(), blacklist);

        if (key.getDependsOn() == MarkerType.NONE) {
          continue;
        }

        addTransformMarker(markers, key.getDependsOn(), interactable.getDependsOnTransform(), blacklist);
      }
    }
  }

  private static void addTransformMarker(
      Map<MarkerType, List<Vec3d>> markers,
      MarkerType markerType,
      BlockTransform<?> transform,
      Set<UUID> blacklist) {
    addMarker(markers, markerType, transform.ensureId(), toMarkerPosition(transform.getPosition()), blacklist);
  }

  private static void addAccessOffsetMarker(
      Map<MarkerType, List<Vec3d>> markers,
      MarkerType markerType,
      BlockTransform<Vec3i> transform,
      Set<UUID> blacklist) {
    Vec3f accessPosition = Vec3f.from(transform.getPosition()).add(transform.getAccessOffset());

    addMarker(markers, markerType, transform.ensureId(), accessPosition.to3d(), blacklist);
  }

  private static void addMarker(
      Map<MarkerType, List<Vec3d>> markers,
      MarkerType markerType,
      UUID waypointId,
      Vec3d position,
      Set<UUID> blacklist) {
    if (blacklist.contains(waypointId)) {
      return;
    }

    markers.computeIfAbsent(markerType, key -> new ArrayList<>()).add(position);
  }

  private static Vec3d toMarkerPosition(Object position) {
    if (position instanceof Vec3d vec3d) {
      return Vec3d.from(vec3d);
    }
    if (position instanceof Vec3f vec3f) {
      return vec3f.to3d();
    }
    if (position instanceof Vec3i vec3i) {
      return vec3i.to3d();
    }
    throw new IllegalArgumentException("Unsupported marker position type: " + position.getClass().getName());
  }

  private static void drawMarkers(World world, Map<MarkerType, List<Vec3d>> markers) {
    for (Entry<MarkerType, List<Vec3d>> markerTypeEntry : markers.entrySet()) {
      MarkerType key = markerTypeEntry.getKey();

      for (Vec3d marker : markerTypeEntry.getValue()) {
        Matrix4d matrix = new Matrix4d();
        matrix.identity();
        matrix.translate(marker.x, marker.y, marker.z);
        matrix.scale(MARKER_SCALE, MARKER_SCALE, MARKER_SCALE);

        DrawUtils.add(
            world,
            key.getShape(),
            matrix,
            key.getColor(),
            DrawUtils.FOREVER,
            DebugUtils.FLAG_NONE);
      }
    }
  }

}
