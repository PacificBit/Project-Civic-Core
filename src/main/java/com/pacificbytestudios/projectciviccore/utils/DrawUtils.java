package com.pacificbytestudios.projectciviccore.utils;

import javax.annotation.Nonnull;

import org.joml.Matrix4d;

import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;

public class DrawUtils extends DebugUtils {
  public static final Vec3f COLOR_BLACK = new Vec3f(0.0F, 0.0F, 0.0F);
  public static final Vec3f COLOR_WHITE = new Vec3f(1.0F, 1.0F, 1.0F);
  public static final Vec3f COLOR_RED = new Vec3f(1.0F, 0.0F, 0.0F);
  public static final Vec3f COLOR_LIME = new Vec3f(0.0F, 1.0F, 0.0F);
  public static final Vec3f COLOR_BLUE = new Vec3f(0.0F, 0.0F, 1.0F);
  public static final Vec3f COLOR_YELLOW = new Vec3f(1.0F, 1.0F, 0.0F);
  public static final Vec3f COLOR_CYAN = new Vec3f(0.0F, 1.0F, 1.0F);
  public static final Vec3f COLOR_MAGENTA = new Vec3f(1.0F, 0.0F, 1.0F);
  public static final Vec3f COLOR_SILVER = new Vec3f(0.75F, 0.75F, 0.75F);
  public static final Vec3f COLOR_GRAY = new Vec3f(0.5F, 0.5F, 0.5F);
  public static final Vec3f COLOR_MAROON = new Vec3f(0.5F, 0.0F, 0.0F);
  public static final Vec3f COLOR_OLIVE = new Vec3f(0.5F, 0.5F, 0.0F);
  public static final Vec3f COLOR_GREEN = new Vec3f(0.0F, 0.5F, 0.0F);
  public static final Vec3f COLOR_PURPLE = new Vec3f(0.5F, 0.0F, 0.5F);
  public static final Vec3f COLOR_TEAL = new Vec3f(0.0F, 0.5F, 0.5F);
  public static final Vec3f COLOR_NAVY = new Vec3f(0.0F, 0.0F, 0.5F);

  public static final float FOREVER = Float.MAX_VALUE;

  public static void addCuboid(
      @Nonnull World world,
      @Nonnull Vec3d center,
      @Nonnull Vec3d size,
      @Nonnull Vec3f color,
      float opacity,
      float seconds) {
    addCuboid(world, center, size, color, opacity, seconds, DebugUtils.FLAG_NONE);
  }

  public static void addCuboid(
      @Nonnull World world,
      @Nonnull Vec3d center,
      @Nonnull Vec3d size,
      @Nonnull Vec3f color,
      float opacity,
      float seconds,
      int flags) {
    Vec3d halfSize = new Vec3d(size).mul(0.5);

    Vec3d min = new Vec3d(center).sub(halfSize);
    Vec3d max = new Vec3d(center).add(halfSize);

    // TODO: Understand the logic of the spawn prefab on the actual hytale grid
    // FIXME: Ceil doesn't work when the tavern spawns on the opposite side of the
    // street
    min.set(Math.ceil(min.x), Math.ceil(min.y), Math.ceil(min.z));
    max.set(Math.ceil(max.x), Math.ceil(max.y), Math.ceil(max.z));

    Vec3d alignedCenter = new Vec3d(min).add(max).mul(0.5);
    Vec3d alignedSize = new Vec3d(max).sub(min);

    Matrix4d matrix = new Matrix4d().translation(alignedCenter).scale(alignedSize.x, alignedSize.y, alignedSize.z);
    add(world, DebugShape.Cube, matrix, color, opacity, seconds, flags);
  }
}
