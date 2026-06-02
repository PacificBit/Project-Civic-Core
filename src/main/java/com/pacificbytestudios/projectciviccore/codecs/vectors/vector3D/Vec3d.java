package com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D;

import javax.annotation.Nonnull;

import org.joml.Vector3dc;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3dUtil;

public class Vec3d extends org.joml.Vector3d {
  public static BuilderCodec<Vec3d> CODEC = BuilderCodec
      .builder(Vec3d.class, Vec3d::new, Vector3dUtil.CODEC)
      .build();

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

  @Override
  public String toString() {
    return "(x: %d, y: %d, z: %d)".formatted(this.x, this.y, this.z);
  }

  @Override
  public Vec3d clone() {
    return new Vec3d(this);
  }
}
