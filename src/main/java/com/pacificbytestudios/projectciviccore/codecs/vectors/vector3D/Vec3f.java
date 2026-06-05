package com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D;

import javax.annotation.Nonnull;

import org.joml.Vector3fc;
import org.joml.Vector3ic;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3fUtil;
import com.hypixel.hytale.math.vector.Vector3iUtil;

public class Vec3f extends org.joml.Vector3f {
  private static final long serialVersionUID = 1L;

  public static BuilderCodec<Vec3f> CODEC = BuilderCodec
      .builder(Vec3f.class, Vec3f::new, Vector3fUtil.CODEC)
      .build();

  public static Vec3f NORTH = Vec3f.from(Vector3iUtil.NORTH);
  public static Vec3f SOUTH = Vec3f.from(Vector3iUtil.SOUTH);
  public static Vec3f WEST = Vec3f.from(Vector3iUtil.WEST);
  public static Vec3f EAST = Vec3f.from(Vector3iUtil.EAST);
  public static Vec3f UP = Vec3f.from(Vector3iUtil.UP);
  public static Vec3f DOWN = Vec3f.from(Vector3iUtil.DOWN);
  public static Vec3f ZERO = Vec3f.from(Vector3iUtil.ZERO);

  public static Vec3f safeNorth() {
    return new Vec3f(Vector3iUtil.NORTH);
  }

  public static Vec3f safeSouth() {
    return new Vec3f(Vector3iUtil.SOUTH);
  }

  public static Vec3f safeWest() {
    return new Vec3f(Vector3iUtil.WEST);
  }

  public static Vec3f safeEast() {
    return new Vec3f(Vector3iUtil.EAST);
  }

  public static Vec3f safeUp() {
    return new Vec3f(Vector3iUtil.UP);
  }

  public static Vec3f safeDown() {
    return new Vec3f(Vector3iUtil.DOWN);
  }

  public static Vec3f safeZero() {
    return new Vec3f(Vector3iUtil.ZERO);
  }

  public static Vec3f[] cardinalDirections() {
    return new Vec3f[] {
        safeNorth(),
        safeEast(),
        safeSouth(),
        safeWest()
    };
  }

  public Vec3f() {
  }

  public Vec3f(Vec3f o) {
    super(o);
  }

  public Vec3f(Vector3fc o) {
    super(o);
  }

  public Vec3f(Vector3ic o) {
    super(o);
  }

  public Vec3f(float d) {
    super(d);
  }

  public Vec3f(float x, float y, float z) {
    super(x, y, z);
  }

  public Vec3f(float[] xyz) {
    super(xyz);
  }

  @Nonnull
  public Vec3f safeAdd(@Nonnull Vec3f vector) {
    Vec3f result = new Vec3f();
    result.x = this.x + vector.x;
    result.y = this.y + vector.y;
    result.z = this.z + vector.z;
    return result;
  }

  @Nonnull
  public Vec3f safeSub(@Nonnull Vec3f Vec3f) {
    Vec3f result = new Vec3f();
    result.x = this.x - Vec3f.x;
    result.y = this.y - Vec3f.y;
    result.z = this.z - Vec3f.z;
    return result;
  }

  @Nonnull
  public Vec3f safeMul(int multiplier) {
    return safeMul(new Vec3f(multiplier, multiplier, multiplier));
  }

  @Nonnull
  public Vec3f safeMul(@Nonnull Vec3f Vec3f) {
    Vec3f result = new Vec3f();
    result.x = this.x * Vec3f.x;
    result.y = this.y * Vec3f.y;
    result.z = this.z * Vec3f.z;
    return result;
  }

  @Override
  public Vec3f add(Vector3fc vector) {
    super.add(vector);
    return this;
  }

  @Override
  public Vec3f add(float x, float y, float z) {
    super.add(x, y, z);
    return this;
  }

  @Override
  public Vec3f sub(Vector3fc vector) {
    super.sub(vector);
    return this;
  }

  @Override
  public Vec3f sub(float x, float y, float z) {
    super.sub(x, y, z);
    return this;
  }

  @Override
  public Vec3f mul(float scalar) {
    super.mul(scalar);
    return this;
  }

  public Vec3d to3d() {
    Vec3d result = new Vec3d();
    result.x = this.x;
    result.y = this.y;
    result.z = this.z;
    return result;
  }

  public Vec3i to3i() {
    return Vec3i.from(this);
  }

  public static Vec3f from(Vec3f vector) {
    if (vector == null) {
      return null;
    }
    return new Vec3f(vector);
  }

  public static Vec3f from(org.joml.Vector3f vector) {
    if (vector == null) {
      return null;
    }
    return new Vec3f(vector);
  }

  public static Vec3f from(Vector3fc vector) {
    if (vector == null) {
      return null;
    }
    return new Vec3f(vector);
  }

  public static Vec3f from(org.joml.Vector3dc vector) {
    if (vector == null) {
      return null;
    }
    return new Vec3f((float) vector.x(), (float) vector.y(), (float) vector.z());
  }

  public static Vec3f from(org.joml.Vector3ic vector) {
    if (vector == null) {
      return null;
    }
    return new Vec3f(vector.x(), vector.y(), vector.z());
  }

  @Override
  public String toString() {
    return "<x: %f, y: %f, z: %f>".formatted(this.x, this.y, this.z);
  }

  @Override
  public Vec3f clone() {
    return new Vec3f(this);
  }
}
