package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints;

import java.util.Map;
import java.util.Map.Entry;

import org.joml.Matrix4d;

import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.utils.DrawUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.Waypoint;

public class WaypointsDrawer {
  private static final double MARKER_SCALE = 0.2;
  // Slightly larger than a game block so the cube stays visible around a placed block.
  private static final double BLOCK_SCALE = 1.02;

  public static void draw(World world, Map<MarkerType, Waypoint[]> waypoints) {
    for (Entry<MarkerType, Waypoint[]> entry : waypoints.entrySet()) {
      drawMarkers(world, entry.getKey(), entry.getValue());
    }
  }

  private static void drawMarkers(
      World world,
      MarkerType markerType,
      Waypoint[] waypoints) {
    for (Waypoint waypoint : waypoints) {
      drawMarker(world, markerType, waypoint);
    }
  }

  private static void drawMarker(
      World world,
      MarkerType markerType,
      Waypoint waypoint) {
    Vec3d position = waypoint.getWaypointPosition();

    if (isBlockWaypoint(waypoint)) {
      drawBlock(world, markerType, position);
      return;
    }

    drawPoint(world, markerType, position);
  }

  /** Block waypoints are drawn as a block-sized cube covering the block they place. */
  private static void drawBlock(
      World world,
      MarkerType markerType,
      Vec3d blockPosition) {
    Matrix4d matrix = new Matrix4d();
    matrix.translation(
        blockPosition.x + 0.5,
        blockPosition.y + 0.5,
        blockPosition.z + 0.5);
    matrix.scale(BLOCK_SCALE, BLOCK_SCALE, BLOCK_SCALE);

    DrawUtils.add(
        world,
        DebugShape.Cube,
        matrix,
        markerType.getColor(),
        DrawUtils.FOREVER,
        DebugUtils.FLAG_NONE);
  }

  private static void drawPoint(
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

  private static boolean isBlockWaypoint(Waypoint waypoint) {
    return !waypoint.isAccess() && !waypoint.isFloatTransform();
  }
}
