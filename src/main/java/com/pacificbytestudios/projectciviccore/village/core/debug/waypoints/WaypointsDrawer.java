package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints;

import java.util.Map;
import java.util.Map.Entry;

import org.joml.Matrix4d;
import org.joml.Vector3ic;

import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
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
      drawBlock(world, markerType, waypoint, position);
      return;
    }

    drawPoint(world, markerType, position);
  }

  /** Block waypoints are drawn as a block-sized cube covering the block they place. */
  private static void drawBlock(
      World world,
      MarkerType markerType,
      Waypoint waypoint,
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

    Rotation rotation = waypoint.getTransform() == null ? null : waypoint.getTransform().getRotation();
    if (rotation != null) {
      drawRotationArrow(world, blockPosition, rotation);
    }
  }

  /**
   * Draws the facing of a rotated block as an arrow hovering above it: a shaft over the
   * block center pointing towards the rotation axis direction, tipped with a cone laid
   * down to point the same way.
   */
  private static void drawRotationArrow(
      World world,
      Vec3d blockPosition,
      Rotation rotation) {
    Vector3ic direction = rotation.getAxisDirection();
    double centerX = blockPosition.x + 0.5;
    double centerZ = blockPosition.z + 0.5;
    double hoverY = blockPosition.y + 1.65;

    Matrix4d shaft = new Matrix4d();
    shaft.translation(
        centerX + direction.x() * 0.3,
        hoverY,
        centerZ + direction.z() * 0.3);
    shaft.scale(
        Math.abs(direction.x()) * 0.6 + 0.07,
        0.07,
        Math.abs(direction.z()) * 0.6 + 0.07);

    DrawUtils.add(
        world,
        DebugShape.Cube,
        shaft,
        DrawUtils.COLOR_WHITE,
        DrawUtils.FOREVER,
        DebugUtils.FLAG_NONE);

    // The cone shape points up (+Y); lay it down so the apex points the arrow direction.
    Matrix4d tip = new Matrix4d();
    tip.translation(
        centerX + direction.x() * 0.75,
        hoverY,
        centerZ + direction.z() * 0.75);
    if (direction.x() != 0) {
      tip.rotateZ(-direction.x() * Math.PI / 2.0);
    } else {
      tip.rotateX(direction.z() * Math.PI / 2.0);
    }
    tip.scale(0.22, 0.3, 0.22);

    DrawUtils.add(
        world,
        DebugShape.Cone,
        tip,
        DrawUtils.COLOR_WHITE,
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
