package com.pacificbytestudios.projectciviccore.utils;

import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import com.hypixel.hytale.math.vector.Vector3iUtil;

public class VectorUtil {

  // =============================
  // Safe constants
  // =============================

  public static Vector3i NORTH() {
    return clone(Vector3iUtil.NORTH);
  }

  public static Vector3i EAST() {
    return clone(Vector3iUtil.EAST);
  }

  public static Vector3i SOUTH() {
    return clone(Vector3iUtil.SOUTH);
  }

  public static Vector3i WEST() {
    return clone(Vector3iUtil.WEST);
  }

  public static Vector3i ZERO() {
    return clone(Vector3iUtil.ZERO);
  }

  public static Vector3i[] cardinalDirections() {
    return new Vector3i[] {
        NORTH(),
        EAST(),
        SOUTH(),
        WEST()
    };
  }

  // =============================
  // Simple Clone
  // =============================

  public static Vector3i clone(Vector3i origin) {
    if (origin == null) {
      return null;
    }
    return new Vector3i(origin.x, origin.y, origin.z);
  }

  public static Vector3f clone(Vector3f origin) {
    if (origin == null) {
      return null;
    }
    return new Vector3f(origin.x, origin.y, origin.z);
  }

  public static Vector3d clone(Vector3d origin) {
    if (origin == null) {
      return null;
    }
    return new Vector3d(origin.x, origin.y, origin.z);
  }

  public static Vector2i clone(Vector2i origin) {
    if (origin == null) {
      return null;
    }
    return new Vector2i(origin.x, origin.y);
  }

  public static Vector3i clone(Vector3ic origin) {
    if (origin == null) {
      return null;
    }
    return new Vector3i(origin.x(), origin.y(), origin.z());
  }

  // =============================
  // Clone Scaled
  // =============================

  public static Vector3f cloneScaled(Vector3f origin, float scale) {
    if (origin == null) {
      return null;
    }
    return VectorUtil.clone(origin).mul(scale);
  }

  public static Vector3f cloneScaled(Vector3i origin, float scale) {
    return cloneScaled(VectorUtil.toVector3f(origin), scale);
  }

  public static Vector3f cloneScaled(Vector3d origin, float scale) {
    return cloneScaled(VectorUtil.toVector3f(origin), scale);
  }

  // =============================
  // To vector 3d
  // =============================

  public static Vector3d toVector3d(Vector3f origin) {
    if (origin == null) {
      return null;
    }
    return new Vector3d(origin.x, origin.y, origin.z);
  }

  public static Vector3d toVector3d(Vector3i origin) {
    if (origin == null) {
      return null;
    }
    return new Vector3d(origin.x, origin.y, origin.z);
  }

  // =============================
  // To vector 3f
  // =============================

  public static Vector3f toVector3f(Vector3i origin) {
    if (origin == null) {
      return null;
    }
    return new Vector3f(origin.x, origin.y, origin.z);
  }

  public static Vector3f toVector3f(Vector3d origin) {
    if (origin == null) {
      return null;
    }
    return new Vector3f((float) origin.x, (float) origin.y, (float) origin.z);
  }

  // =============================
  // To vector 3i
  // =============================

  public static Vector3i toVector3i(Vector3f origin) {
    if (origin == null) {
      return null;
    }
    return new Vector3i((int) origin.x, (int) origin.y, (int) origin.z);
  }

  public static Vector3i toVector3i(Vector3d origin) {
    if (origin == null) {
      return null;
    }
    return new Vector3i((int) origin.x, (int) origin.y, (int) origin.z);
  }

  // =============================
  // To string
  // =============================

  public static String toString(Vector3i point) {
    return "<x: " + point.x + ", y: " + point.y + ", z: " + point.z + ">";
  }

  public static String toString(Vector3d point) {
    return "<x: " + point.x + ", y: " + point.y + ", z: " + point.z + ">";
  }

  public static String toString(Vector3f point) {
    return "<x: " + point.x + ", y: " + point.y + ", z: " + point.z + ">";
  }
}
