package com.pacificbytestudios.projectciviccore.codecs;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public class AreaFootprint implements Component<ChunkStore> {

  public static final BuilderCodec<AreaFootprint> CODEC = BuilderCodec
      .builder(AreaFootprint.class, AreaFootprint::new)

      .append(new KeyedCodec<>("TopLeft", Vector2i.CODEC),
          AreaFootprint::setTopLeft,
          (obj) -> obj.topLeft)
      .add()

      .append(new KeyedCodec<>("BotRight", Vector2i.CODEC),
          AreaFootprint::setBotRight,
          (obj) -> obj.botRight)
      .add()

      .append(new KeyedCodec<>("Y", Codec.INTEGER),
          (obj, value) -> obj.y = value,
          (obj) -> obj.y)
      .add()

      .build();

  private Vector2i topLeft;
  private Vector2i botRight;
  private int y;

  public AreaFootprint() {
  }

  public AreaFootprint(Vector2i topLeft, Vector2i botRight) {
    assignBounds(topLeft, botRight);
  }

  public AreaFootprint(AreaFootprint offset, Vector3i center) {
    this(offset, center, Rotation.None);
  }

  public AreaFootprint(AreaFootprint offset, Vector3i center, Rotation rotation) {
    if (offset == null || offset.topLeft == null || offset.botRight == null ||
        center == null) {
      return;
    }

    Rotation normalizedRotation = rotation == null ? Rotation.None : rotation;
    Vector3i rotatedTopLeft = normalizedRotation.rotateY(
        new Vector3i(offset.topLeft.x, 0, offset.topLeft.y),
        new Vector3i());
    Vector3i rotatedBotRight = normalizedRotation.rotateY(
        new Vector3i(offset.botRight.x, 0, offset.botRight.y),
        new Vector3i());

    this.topLeft = new Vector2i(center.getX() + rotatedTopLeft.x, center.getZ() +
        rotatedTopLeft.z);
    this.botRight = new Vector2i(center.getX() + rotatedBotRight.x, center.getZ()
        + rotatedBotRight.z);
    this.y = center.y;
  }

  protected void setY(int y) {
    this.y = y;
  }

  protected void assignBounds(Vector2i topLeft, Vector2i botRight) {
    if (topLeft == null || botRight == null) {
      this.topLeft = topLeft == null ? null : topLeft.clone();
      this.botRight = botRight == null ? null : botRight.clone();
      return;
    }

    this.topLeft = topLeft.clone();
    this.botRight = botRight.clone();
  }

  @Override
  public AreaFootprint clone() {
    AreaFootprint claimArea = new AreaFootprint();
    claimArea.topLeft = this.topLeft == null ? null : this.topLeft.clone();
    claimArea.botRight = this.botRight == null ? null : this.botRight.clone();
    claimArea.y = this.y;
    return claimArea;
  }

  public Vector2i getTopLeft() {
    return topLeft;
  }

  public Vector3i getTopLeft3i() {
    return new Vector3i(this.topLeft.x, this.y, this.topLeft.y);
  }

  public void setTopLeft(Vector2i topLeft) {
    this.topLeft = topLeft == null ? null : topLeft.clone();
  }

  public Vector2i getBotRight() {
    return botRight;
  }

  public Vector3i getBotRight3i() {
    return new Vector3i(this.botRight.x, this.y, this.botRight.y);
  }

  public void setBotRight(Vector2i botRight) {
    this.botRight = botRight == null ? null : botRight.clone();
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

  public Vector2i getAxisAlignedTopLeft() {
    if (this.topLeft == null || this.botRight == null) {
      return null;
    }

    return new Vector2i(getMinX(), getMinZ());
  }

  public Vector2i getAxisAlignedBotRight() {
    if (this.topLeft == null || this.botRight == null) {
      return null;
    }

    return new Vector2i(getMaxX(), getMaxZ());
  }

  public Vector2i getCenter2D() {
    if (this.topLeft == null || this.botRight == null) {
      return null;
    }

    int centerX = getMinX() + ((getMaxX() - getMinX()) / 2);
    int centerZ = getMinZ() + ((getMaxZ() - getMinZ()) / 2);
    return new Vector2i(centerX, centerZ);
  }

  public ComponentType<ChunkStore, AreaFootprint> getComponentType() {
    return ProjectCivicCore.get().getClaimedAreaComponent();
  }

  public boolean isInside(Vector2i coord2d) {
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
    return "ClaimedAreaComponent{topLeft=" + this.topLeft + ", botRight=" +
        this.botRight + "}";
  }
}
