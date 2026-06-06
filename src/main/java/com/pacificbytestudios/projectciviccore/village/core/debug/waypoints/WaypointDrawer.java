package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.joml.Matrix4d;

import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;
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

public class WaypointDrawer {
  private static final double MARKER_SCALE = 0.2;

  public static void draw(World world, WorkplaceComponent workplace, List<Vec3i> blacklist) {
    Map<MarkerType, List<Vec3d>> markers = initMarkerMap();

    addWorkplaceMarkers(workplace, markers, blacklist);
    drawMarkers(world, markers);
  }

  private static Map<MarkerType, List<Vec3d>> initMarkerMap() {
    Map<MarkerType, List<Vec3d>> markers = new HashMap<>();
    Stream.of(MarkerType.values())
        .forEach(markerType -> markers.put(markerType, new ArrayList<>()));
    return markers;
  }

  private static void addWorkplaceMarkers(
      WorkplaceComponent workplace,
      Map<MarkerType, List<Vec3d>> markers,
      List<Vec3i> blacklist) {
    addMarker(markers, MarkerType.BENCH, workplace.getBenchPos(), blacklist);
    addMarker(markers, MarkerType.BENCH_ACCESS, workplace.getBenchAccessPos(), blacklist);
    addMarker(markers, MarkerType.ANCHOR, workplace.getPrefabAnchor(), blacklist);

    WorkplaceContainerRegistryComponent storage = workplace.getStorage();
    if (storage != null) {
      storage.getAllContainerPositions()
          .forEach(containerPos -> addMarker(markers, MarkerType.STORAGE, containerPos, blacklist));

      storage.getAllContainerAccessPositions()
          .forEach(containerAccessPos -> addMarker(markers, MarkerType.STORAGE_ACCESS, containerAccessPos, blacklist));
    }

    WorkplaceInteractablesMapComponent interactables = workplace.getComponent(
        WorkplaceInteractablesMapComponent.getComponentType());
    if (interactables != null) {
      addInteractables(interactables, markers, blacklist);
    }
  }

  private static void addInteractables(
      WorkplaceInteractablesMapComponent workplaceInteractables,
      Map<MarkerType, List<Vec3d>> markers,
      List<Vec3i> blacklist) {
    Map<InteractablesType, Interactable[]> interactableMap = workplaceInteractables.getAllInteractables();

    for (Entry<InteractablesType, Interactable[]> interactableEntry : interactableMap.entrySet()) {
      InteractablesType key = interactableEntry.getKey();
      Interactable[] interactables = interactableEntry.getValue();
      // if (key == null || interactables == null) {
      // continue;
      // }

      for (Interactable interactable : interactables) {
        // if (interactable == null) {
        // continue;
        // }

        addMarker(markers, key.getMarkerType(), interactable.getInteractablePosition(), blacklist);

        if (key.getDependsOn().equals(MarkerType.NONE)) {
          continue;
        }

        addMarker(markers, key.getDependsOn(), interactable.getDependsOn(), blacklist);
      }
    }
  }

  private static void addMarker(
      Map<MarkerType, List<Vec3d>> markers,
      MarkerType markerType,
      Vec3i position,
      List<Vec3i> blacklist) {
    if (position == null || isBlacklisted(position, blacklist)) {
      return;
    }

    markers.get(markerType).add(position.to3d());
  }

  private static void addMarker(
      Map<MarkerType, List<Vec3d>> markers,
      MarkerType markerType,
      Vec3f position,
      List<Vec3i> blacklist) {
    if (position == null || isBlacklisted(position, blacklist)) {
      return;
    }

    markers.get(markerType).add(position.to3d());
  }

  private static boolean isBlacklisted(Vec3i position, List<Vec3i> blacklist) {
    return blacklist.contains(position);
  }

  private static boolean isBlacklisted(Vec3f position, List<Vec3i> blacklist) {
    return blacklist.contains(position.to3i());
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
