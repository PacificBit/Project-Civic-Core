package com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D;

import javax.annotation.Nonnull;

import org.joml.Vector3fc;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3fUtil;

public class Vec3f extends org.joml.Vector3f {
  public static BuilderCodec<Vec3f> CODEC = BuilderCodec
      .builder(Vec3f.class, Vec3f::new, Vector3fUtil.CODEC)
      .build();

  public Vec3f() {
  }

  public Vec3f(Vec3f o) {
    super(o);
  }

  public Vec3f(Vector3fc o) {
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
    return safeAdd(vector.x, vector.y, vector.z);
  }

  @Nonnull
  public Vec3f safeAdd(float x, float y, float z) {
    Vec3f result = new Vec3f();
    result.x = this.x + x;
    result.y = this.y + y;
    result.z = this.z + z;
    return result;
  }

  @Nonnull
  public Vec3f safeSub(@Nonnull Vec3f vector) {
    return safeSub(vector.x, vector.y, vector.z);
  }

  @Nonnull
  public Vec3f safeSub(float x, float y, float z) {
    Vec3f result = new Vec3f();
    result.x = this.x - x;
    result.y = this.y - y;
    result.z = this.z - z;
    return result;
  }

  @Nonnull
  public Vec3f safeMul(int multiplier) {
    return safeMul(new Vec3f(multiplier, multiplier, multiplier));
  }

  @Nonnull
  public Vec3f safeMul(@Nonnull Vec3f vector) {
    return safeMul(vector.x, vector.y, vector.z);
  }

  @Nonnull
  public Vec3f safeMul(float x, float y, float z) {
    Vec3f result = new Vec3f();
    result.x = this.x * x;
    result.y = this.y * y;
    result.z = this.z * z;
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
    Vec3i result = new Vec3i();
    result.x = (int) this.x;
    result.y = (int) this.y;
    result.z = (int) this.z;
    return result;
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
