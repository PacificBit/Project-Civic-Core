package com.pacificbytestudios.projectciviccore.village.core.debug.waypoints;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector3ic;

import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.utils.DrawUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;
import com.pacificbytestudios.projectciviccore.village.core.debug.waypoints.components.Waypoint;

/**
 * Single source of truth for how structure waypoints are visualized. Both the waypoint
 * editing session and the debug draw command go through here so every visualization looks
 * identical: block waypoints as a block-sized cube (plus a facing arrow when rotated) and
 * point waypoints as their marker shape.
 */
public final class DebugDrawUtils {
  private static final double MARKER_SCALE = 0.2;
  // Slightly larger than a game block so the cube stays visible around a placed block.
  private static final double BLOCK_SCALE = 1.02;
  // How high above the block the facing arrow hovers, and how long it is.
  private static final double ARROW_HOVER = 1.65;
  private static final double ARROW_LENGTH = 0.9;

  private DebugDrawUtils() {
  }

  public static void draw(World world, Map<MarkerType, List<Waypoint>> waypoints) {
    for (Entry<MarkerType, List<Waypoint>> entry : waypoints.entrySet()) {
      drawMarkers(world, entry.getKey(), entry.getValue());
    }
  }

  private static void drawMarkers(World world, MarkerType markerType, List<Waypoint> waypoints) {
    for (Waypoint waypoint : waypoints) {
      if (waypoint != null) {
        drawMarker(world, markerType, waypoint);
      }
    }
  }

  private static void drawMarker(World world, MarkerType markerType, Waypoint waypoint) {
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

  /** Draws the facing of a rotated block as an arrow hovering above its center. */
  private static void drawRotationArrow(World world, Vec3d blockPosition, Rotation rotation) {
    Vector3ic axis = rotation.getAxisDirection();
    Vector3d direction = new Vector3d(axis.x(), 0.0, axis.z());
    if (direction.lengthSquared() < 1.0e-6) {
      return;
    }
    direction.normalize().mul(ARROW_LENGTH);

    Vector3d origin = new Vector3d(
        blockPosition.x + 0.5,
        blockPosition.y + ARROW_HOVER,
        blockPosition.z + 0.5);

    DrawUtils.addArrow(
        world,
        origin,
        direction,
        DrawUtils.COLOR_WHITE,
        DrawUtils.FOREVER,
        DebugUtils.FLAG_NONE);
  }

  private static void drawPoint(World world, MarkerType markerType, Vec3d position) {
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
