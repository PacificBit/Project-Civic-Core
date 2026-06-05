package com.pacificbytestudios.projectciviccore.codecs;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D.Vec2i;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

public class AreaFootprint implements Component<ChunkStore> {

  public static final BuilderCodec<AreaFootprint> CODEC = BuilderCodec
      .builder(AreaFootprint.class, AreaFootprint::new)

      .append(new KeyedCodec<>("TopLeft", Vec2i.CODEC),
          AreaFootprint::setTopLeft,
          (obj) -> obj.topLeft)
      .add()

      .append(new KeyedCodec<>("BotRight", Vec2i.CODEC),
          AreaFootprint::setBotRight,
          (obj) -> obj.botRight)
      .add()

      .append(new KeyedCodec<>("Y", Codec.INTEGER),
          (obj, value) -> obj.y = value,
          (obj) -> obj.y)
      .add()

      .build();

  private Vec2i topLeft;
  private Vec2i botRight;
  private int y;

  public AreaFootprint() {
  }

  public AreaFootprint(Vec2i topLeft, Vec2i botRight) {
    this.topLeft = Vec2i.from(topLeft);
    this.botRight = Vec2i.from(botRight);
  }

  public AreaFootprint(AreaFootprint offset, Vec3i center) {
    this(offset, center, Rotation.None);
  }

  public AreaFootprint(AreaFootprint offset, Vec3i center, Rotation rotation) {
    if (offset == null || offset.topLeft == null || offset.botRight == null ||
        center == null) {
      return;
    }

    Rotation normalizedRotation = rotation == null ? Rotation.None : rotation;
    Vec3i rotatedTopLeft = Vec3i.from(normalizedRotation.rotateY(
        new Vec3i(offset.topLeft.x, 0, offset.topLeft.y), new Vec3i()));

    Vec3i rotatedBotRight = Vec3i.from(normalizedRotation.rotateY(
        new Vec3i(offset.botRight.x, 0, offset.botRight.y), new Vec3i()));

    this.topLeft = new Vec2i(center.x + rotatedTopLeft.x, center.z +
        rotatedTopLeft.z);
    this.botRight = new Vec2i(center.x + rotatedBotRight.x, center.z
        + rotatedBotRight.z);
    this.y = center.y;
  }

  protected void setY(int y) {
    this.y = y;
  }

  @Override
  public AreaFootprint clone() {
    AreaFootprint claimArea = new AreaFootprint();
    claimArea.topLeft = Vec2i.from(this.topLeft);
    claimArea.botRight = Vec2i.from(this.botRight);
    claimArea.y = this.y;
    return claimArea;
  }

  public Vec2i getTopLeft() {
    return topLeft;
  }

  public Vec3i getTopLeft3i() {
    return new Vec3i(this.topLeft.x, this.y, this.topLeft.y);
  }

  public void setTopLeft(Vec2i topLeft) {
    this.topLeft = Vec2i.from(topLeft);
  }

  public Vec2i getBotRight() {
    return botRight;
  }

  public Vec3i getBotRight3i() {
    return new Vec3i(this.botRight.x, this.y, this.botRight.y);
  }

  public void setBotRight(Vec2i botRight) {
    this.botRight = Vec2i.from(botRight);
  }

  public int getY() {
    return this.y;
  }

  public int getMinX() {
    return Math.min(this.topLeft.x, this.botRight.x);
  }

  public int getMaxX() {
    return Math.max(this.topLeft.x, this.botRight.x);
  }

  public int getMinZ() {
    return Math.min(this.topLeft.y, this.botRight.y);
  }

  public int getMaxZ() {
    return Math.max(this.topLeft.y, this.botRight.y);
  }

  public Vec2i getAxisAlignedTopLeft() {
    if (this.topLeft == null || this.botRight == null) {
      return null;
    }

    return new Vec2i(getMinX(), getMinZ());
  }

  public Vec2i getAxisAlignedBotRight() {
    if (this.topLeft == null || this.botRight == null) {
      return null;
    }

    return new Vec2i(getMaxX(), getMaxZ());
  }

  public Vec2i getCenter2D() {
    if (this.topLeft == null || this.botRight == null) {
      return null;
    }

    int centerX = getMinX() + ((getMaxX() - getMinX()) / 2);
    int centerZ = getMinZ() + ((getMaxZ() - getMinZ()) / 2);
    return new Vec2i(centerX, centerZ);
  }

  public Vec3i getCenter3D() {
    if (this.topLeft == null || this.botRight == null) {
      return null;
    }

    int centerX = getMinX() + ((getMaxX() - getMinX()) / 2);
    int centerZ = getMinZ() + ((getMaxZ() - getMinZ()) / 2);
    return new Vec3i(centerX, y, centerZ);
  }

  public ComponentType<ChunkStore, AreaFootprint> getComponentType() {
    return ProjectCivicCore.get().getClaimedAreaComponent();
  }

  public boolean isInside(Vec2i coord2d) {
    if (this.topLeft == null || this.botRight == null) {
      return false;
    }
    int x = coord2d.x;
    int z = coord2d.y;

    return x >= getMinX() && x <= getMaxX() && z >= getMinZ() &&
        z <= getMaxZ();
  }

  @Override
  public String toString() {
    return "ClaimedAreaComponent: { TopLeft: %s BotRight: %s }"
        .formatted(this.topLeft.toString(), this.botRight.toString());
  }
}
