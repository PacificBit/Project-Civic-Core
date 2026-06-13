package com.pacificbytestudios.projectciviccore.village.commands.core.utils;

import java.util.Arrays;

import com.hypixel.hytale.protocol.DebugShape;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.utils.DrawUtils;

public enum MarkerType {
  NONE(DebugShape.Sphere, DrawUtils.COLOR_RED),
  ANCHOR(DebugShape.Cone, DrawUtils.COLOR_CYAN),
  ORIGIN(DebugShape.Frustum, DrawUtils.COLOR_MAGENTA),
  BENCH(DebugShape.Cube, DrawUtils.COLOR_OLIVE),
  BENCH_ACCESS(DebugShape.Sphere, DrawUtils.COLOR_GREEN),
  STORAGE(DebugShape.Cube, DrawUtils.COLOR_GRAY),
  STORAGE_ACCESS(DebugShape.Sphere, DrawUtils.COLOR_BLACK),
  TAVERN_CHAIR(DebugShape.Sphere, DrawUtils.COLOR_LIME),
  TAVERN_PLATE(DebugShape.Cylinder, DrawUtils.COLOR_GREEN),
  BED(DebugShape.Sphere, DrawUtils.COLOR_RED);

  private DebugShape shape;
  private Vec3f color;

  public static MarkerType[] DEFAULTS = new MarkerType[] { BENCH, BENCH_ACCESS, STORAGE, STORAGE_ACCESS };

  private MarkerType(DebugShape shape, Vec3f color) {
    this.shape = shape;
    this.color = color;
  }

  public DebugShape getShape() {
    return shape;
  }

  /**
   * Point markers carry sub-block precision and are drawn with their own shape rather than
   * as a block-aligned cube. The classification must come from the marker type, not from
   * whether the position happens to land on whole-block coordinates.
   */
  public boolean isFloatMarker() {
    return this == TAVERN_CHAIR || this == TAVERN_PLATE;
  }

  public Vec3f getColor() {
    return color;
  }

  public static MarkerType[] markersTypeBuilder(MarkerType[] base, MarkerType... extra) {
    var result = Arrays.copyOf(base, base.length + extra.length);
    System.arraycopy(extra, 0, result, base.length, extra.length);
    return result;
  }

}
