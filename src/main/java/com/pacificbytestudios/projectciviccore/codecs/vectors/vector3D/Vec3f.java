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

  @Override
  public String toString() {
    return "(x: %d, y: %d, z: %d)".formatted(this.x, this.y, this.z);
  }

  @Override
  public Vec3f clone() {
    return new Vec3f(this);
  }
}
