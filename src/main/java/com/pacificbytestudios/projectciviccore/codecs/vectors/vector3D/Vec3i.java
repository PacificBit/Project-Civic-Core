package com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D;

import javax.annotation.Nonnull;

import org.joml.Vector3ic;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3iUtil;

public class Vec3i extends org.joml.Vector3i {
  private static final long serialVersionUID = 1L;

  public static BuilderCodec<Vec3i> CODEC = BuilderCodec
      .builder(Vec3i.class, Vec3i::new, Vector3iUtil.CODEC)
      .build();

  public static Vec3i NORTH = Vec3i.from(Vector3iUtil.NORTH);
  public static Vec3i SOUTH = Vec3i.from(Vector3iUtil.SOUTH);
  public static Vec3i WEST = Vec3i.from(Vector3iUtil.WEST);
  public static Vec3i EAST = Vec3i.from(Vector3iUtil.EAST);
  public static Vec3i UP = Vec3i.from(Vector3iUtil.UP);
  public static Vec3i DOWN = Vec3i.from(Vector3iUtil.DOWN);
  public static Vec3i ZERO = Vec3i.from(Vector3iUtil.ZERO);

  public static Vec3i safeNorth() {
    return new Vec3i(Vector3iUtil.NORTH);
  }

  public static Vec3i safeSouth() {
    return new Vec3i(Vector3iUtil.SOUTH);
  }

  public static Vec3i safeWest() {
    return new Vec3i(Vector3iUtil.WEST);
  }

  public static Vec3i safeEast() {
    return new Vec3i(Vector3iUtil.EAST);
  }

  public static Vec3i safeUp() {
    return new Vec3i(Vector3iUtil.UP);
  }

  public static Vec3i safeDown() {
    return new Vec3i(Vector3iUtil.DOWN);
  }

  public static Vec3i safeZero() {
    return new Vec3i(Vector3iUtil.ZERO);
  }

  public static Vec3i[] cardinalDirections() {
    return new Vec3i[] {
        safeNorth(),
        safeEast(),
        safeSouth(),
        safeWest()
    };
  }

  public static Vec3i from(org.joml.Vector3i vector) {
    if (vector == null) {
      return null;
    }
    return new Vec3i(vector);
  }

  public static Vec3i from(org.joml.Vector3ic vector) {
    if (vector == null) {
      return null;
    }
    return new Vec3i(vector);
  }

  public static Vec3i from(org.joml.Vector3fc vector) {
    if (vector == null) {
      return null;
    }
    return new Vec3i(
        (int) Math.floor(vector.x()),
        (int) Math.floor(vector.y()),
        (int) Math.floor(vector.z()));
  }

  public static Vec3i from(org.joml.Vector3d vector3d) {
    if (vector3d == null) {
      return null;
    }
    Vec3i result = new Vec3i();
    result.x = (int) Math.floor(vector3d.x);
    result.y = (int) Math.floor(vector3d.y);
    result.z = (int) Math.floor(vector3d.z);
    return result;
  }

  public static Vec3i from(org.joml.Vector3dc vector) {
    if (vector == null) {
      return null;
    }
    return new Vec3i(
        (int) Math.floor(vector.x()),
        (int) Math.floor(vector.y()),
        (int) Math.floor(vector.z()));
  }

  public static Vec3i from(Vec3i vector) {
    if (vector == null) {
      return null;
    }
    return new Vec3i(vector);
  }

  public Vec3i() {
  }

  public Vec3i(Vec3i v) {
    super(v);
  }

  public Vec3i(Vector3ic v) {
    super(v);
  }

  public Vec3i(int d) {
    super(d);
  }

  public Vec3i(int x, int y, int z) {
    super(x, y, z);
  }

  public Vec3i(int[] xyz) {
    super(xyz);
  }

  @Nonnull
  public Vec3i safeAdd(@Nonnull Vec3i vector) {
    Vec3i result = new Vec3i();
    result.x = this.x + vector.x;
    result.y = this.y + vector.y;
    result.z = this.z + vector.z;
    return result;
  }

  @Nonnull
  public Vec3i safeSub(@Nonnull Vec3i vector3i) {
    Vec3i result = new Vec3i();
    result.x = this.x - vector3i.x;
    result.y = this.y - vector3i.y;
    result.z = this.z - vector3i.z;
    return result;
  }

  @Nonnull
  public Vec3i safeMul(int multiplier) {
    return safeMul(new Vec3i(multiplier, multiplier, multiplier));
  }

  @Nonnull
  public Vec3i safeMul(@Nonnull Vec3i vector3i) {
    Vec3i result = new Vec3i();
    result.x = this.x * vector3i.x;
    result.y = this.y * vector3i.y;
    result.z = this.z * vector3i.z;
    return result;
  }

  public Vec3i add(Vec3i vector) {
    this.x += vector.x;
    this.y += vector.y;
    this.z += vector.z;
    return this;
  }

  @Override
  public Vec3i add(Vector3ic vector) {
    super.add(vector);
    return this;
  }

  public Vec3i add(org.joml.Vector3i vector) {
    super.add(vector);
    return this;
  }

  @Override
  public Vec3i add(int x, int y, int z) {
    super.add(x, y, z);
    return this;
  }

  @Override
  public Vec3i sub(Vector3ic vector) {
    super.sub(vector);
    return this;
  }

  @Override
  public Vec3i sub(int x, int y, int z) {
    super.sub(x, y, z);
    return this;
  }

  @Override
  public Vec3i mul(int scalar) {
    super.mul(scalar);
    return this;
  }

  @Override
  public Vec3i mul(Vector3ic vector) {
    super.mul(vector);
    return this;
  }

  public Vec3d to3d() {
    Vec3d result = new Vec3d();
    result.x = this.x;
    result.y = this.y;
    result.z = this.z;
    return result;
  }

  public Vec3f to3f() {
    Vec3f result = new Vec3f();
    result.x = this.x;
    result.y = this.y;
    result.z = this.z;
    return result;
  }

  @Override
  public String toString() {
    return "<x: %d, y: %d, z: %d>".formatted(this.x, this.y, this.z);
  }

  @Override
  public Vec3i clone() {
    return new Vec3i(this);
  }
}
