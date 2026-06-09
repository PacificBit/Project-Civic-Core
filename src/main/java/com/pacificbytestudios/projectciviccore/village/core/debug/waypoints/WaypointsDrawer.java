package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints;

import java.util.Map;
import java.util.Map.Entry;

import org.joml.Matrix4d;

import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.DrawUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;

public class WaypointsDrawer {
  private static final double MARKER_SCALE = 0.2;

  public static void draw(World world, Map<MarkerType, BlockTransform<Vec3i>[]> waypoints) {
    for (Entry<MarkerType, BlockTransform<Vec3i>[]> entry : waypoints.entrySet()) {
      drawMarkers(world, entry.getKey(), entry.getValue());
    }
  }

  private static void drawMarkers(
      World world,
      MarkerType markerType,
      BlockTransform<Vec3i>[] waypoints) {
    for (BlockTransform<Vec3i> waypoint : waypoints) {
      drawMarker(world, markerType, waypointPosition(markerType, waypoint));
    }
  }

  private static Vec3d waypointPosition(MarkerType markerType, BlockTransform<Vec3i> waypoint) {
    if (isAccessMarker(markerType)) {
      return waypoint.getAccessOffset().to3d();
    }
    return waypoint.getPosition().to3d();
  }

  private static boolean isAccessMarker(MarkerType markerType) {
    return switch (markerType) {
      case BENCH_ACCESS, STORAGE_ACCESS -> true;
      default -> false;
    };
  }

  private static void drawMarker(
      World world,
      MarkerType markerType,
      Vec3d position) {
    Matrix4d matrix = new Matrix4d();
    matrix.identity();
    matrix.translate(position.x, position.y, position.z);
    matrix.scale(MARKER_SCALE, MARKER_SCALE, MARKER_SCALE);

    DrawUtils.add(
        world,
        markerType.getShape(),
        matrix,
        markerType.getColor(),
        DrawUtils.FOREVER,
        DebugUtils.FLAG_NONE);
  }
}
