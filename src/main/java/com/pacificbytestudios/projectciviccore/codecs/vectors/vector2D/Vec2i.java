package com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D;

import org.joml.Vector2ic;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector2iUtil;

public class Vec2i extends org.joml.Vector2i {
  private static final long serialVersionUID = 1L;

  public static BuilderCodec<Vec2i> CODEC = BuilderCodec
      .builder(Vec2i.class, Vec2i::new, Vector2iUtil.CODEC)
      .build();

  public Vec2i() {
  }

  public Vec2i(int s) {
    this.x = s;
    this.y = s;
  }

  public Vec2i(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public Vec2i(Vector2ic v) {
    super(v);
  }

  public Vec2i(int[] xy) {
    super(xy);
  }

  public static Vec2i from(Vec2i vector) {
    if (vector == null) {
      return null;
    }
    return new Vec2i(vector);
  }

  public static Vec2i from(org.joml.Vector2i vector) {
    if (vector == null) {
      return null;
    }
    return new Vec2i(vector);
  }

  public static Vec2i from(Vector2ic vector) {
    if (vector == null) {
      return null;
    }
    return new Vec2i(vector);
  }

  @Override
  public String toString() {
    return "<x: %d, y: %d>".formatted(this.x, this.y);
  }

  @Override
  public Vec2i clone() {
    return new Vec2i(this);
  }
}
