package com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D;

import javax.annotation.Nonnull;

import org.joml.Vector3dc;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3dUtil;

public class Vec3d extends org.joml.Vector3d {
  private static final long serialVersionUID = 1L;

  public static BuilderCodec<Vec3d> CODEC = BuilderCodec
      .builder(Vec3d.class, Vec3d::new, Vector3dUtil.CODEC)
      .build();

  public static Vec3d LEFT = new Vec3d(Vector3dUtil.LEFT);

  public static Vec3d safeLeft() {
    return new Vec3d(Vector3dUtil.LEFT);
  }

  public Vec3d() {
  }

  public Vec3d(Vec3d o) {
    super(o);
  }

  public Vec3d(Vector3dc o) {
    super(o);
  }

  public Vec3d(double d) {
    super(d);
  }

  public Vec3d(double x, double y, double z) {
    super(x, y, z);
  }

  public Vec3d(double[] xyz) {
    super(xyz);
  }

  public Vec3d(float[] xyz) {
    super(xyz);
  }

  @Nonnull
  public Vec3d safeAdd(@Nonnull Vec3d vector) {
    Vec3d result = new Vec3d();
    result.x = this.x + vector.x;
    result.y = this.y + vector.y;
    result.z = this.z + vector.z;
    return result;
  }

  @Nonnull
  public Vec3d safeSub(@Nonnull Vec3d Vec3d) {
    Vec3d result = new Vec3d();
    result.x = this.x - Vec3d.x;
    result.y = this.y - Vec3d.y;
    result.z = this.z - Vec3d.z;
    return result;
  }

  @Nonnull
  public Vec3d safeMul(int multiplier) {
    return safeMul(new Vec3d(multiplier, multiplier, multiplier));
  }

  @Nonnull
  public Vec3d safeMul(@Nonnull Vec3d Vec3d) {
    Vec3d result = new Vec3d();
    result.x = this.x * Vec3d.x;
    result.y = this.y * Vec3d.y;
    result.z = this.z * Vec3d.z;
    return result;
  }

  @Override
  public Vec3d add(Vector3dc vector) {
    super.add(vector);
    return this;
  }

  @Override
  public Vec3d add(double x, double y, double z) {
    super.add(x, y, z);
    return this;
  }

  @Override
  public Vec3d sub(Vector3dc vector) {
    super.sub(vector);
    return this;
  }

  @Override
  public Vec3d sub(double x, double y, double z) {
    super.sub(x, y, z);
    return this;
  }

  @Override
  public Vec3d mul(double scalar) {
    super.mul(scalar);
    return this;
  }

  public Vec3f to3f() {
    Vec3f result = new Vec3f();
    result.x = (float) this.x;
    result.y = (float) this.y;
    result.z = (float) this.z;
    return result;
  }

  public Vec3i to3i() {
    Vec3i result = new Vec3i();
    result.x = (int) this.x;
    result.y = (int) this.y;
    result.z = (int) this.z;
    return result;
  }

  public static Vec3d from(Vec3d vector) {
    if (vector == null) {
      return null;
    }
    return new Vec3d(vector);
  }

  public static Vec3d from(org.joml.Vector3d vector) {
    if (vector == null) {
      return null;
    }
    return new Vec3d(vector);
  }

  public static Vec3d from(Vector3dc vector) {
    if (vector == null) {
      return null;
    }
    return new Vec3d(vector);
  }

  public static Vec3d from(org.joml.Vector3fc vector) {
    if (vector == null) {
      return null;
    }
    return new Vec3d(vector.x(), vector.y(), vector.z());
  }

  public static Vec3d from(org.joml.Vector3ic vector) {
    if (vector == null) {
      return null;
    }
    return new Vec3d(vector.x(), vector.y(), vector.z());
  }

  public static String toString(org.joml.Vector3d vector) {
    if (vector == null) {
      return "null";
    }
    return "<x: %f, y: %f, z: %f>".formatted(vector.x, vector.y, vector.z);
  }

  @Override
  public String toString() {
    return "<x: %f, y: %f, z: %f>".formatted(this.x, this.y, this.z);
  }

  @Override
  public Vec3d clone() {
    return new Vec3d(this);
  }
}
