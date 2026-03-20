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

public class ClaimedAreaComponent implements Component<ChunkStore> {

  public static final BuilderCodec<ClaimedAreaComponent> CODEC = BuilderCodec
      .builder(ClaimedAreaComponent.class, ClaimedAreaComponent::new)

      .append(new KeyedCodec<>("TopLeft", Vector2i.CODEC),
          (obj, value) -> obj.topLeft = value,
          (obj) -> obj.topLeft)
      .add()

      .append(new KeyedCodec<>("BotRight", Vector2i.CODEC),
          (obj, value) -> obj.botRight = value,
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

  public ClaimedAreaComponent() {
  }

  public ClaimedAreaComponent(Vector2i topLeft, Vector2i botRight) {
    assignBounds(topLeft, botRight);
  }

  public ClaimedAreaComponent(ClaimedAreaComponent offset, Vector3i center) {
    this(offset, center, Rotation.None);
  }

  public ClaimedAreaComponent(ClaimedAreaComponent offset, Vector3i center, Rotation rotation) {
    if (offset == null || offset.topLeft == null || offset.botRight == null || center == null) {
      return;
    }

    Rotation normalizedRotation = rotation == null ? Rotation.None : rotation;
    Vector2i[] corners = getCorners(offset.topLeft, offset.botRight);
    int minX = Integer.MAX_VALUE;
    int minZ = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxZ = Integer.MIN_VALUE;

    for (Vector2i corner : corners) {
      Vector3i rotatedCorner = normalizedRotation.rotateY(new Vector3i(corner.x, 0, corner.y), new Vector3i());
      minX = Math.min(minX, center.getX() + rotatedCorner.x);
      minZ = Math.min(minZ, center.getZ() + rotatedCorner.z);
      maxX = Math.max(maxX, center.getX() + rotatedCorner.x);
      maxZ = Math.max(maxZ, center.getZ() + rotatedCorner.z);
    }

    this.topLeft = new Vector2i(minX, minZ);
    this.botRight = new Vector2i(maxX, maxZ);
    this.y = center.y;
  }

  private static Vector2i[] getCorners(Vector2i topLeft, Vector2i botRight) {
    int minX = Math.min(topLeft.x, botRight.x);
    int minZ = Math.min(topLeft.y, botRight.y);
    int maxX = Math.max(topLeft.x, botRight.x);
    int maxZ = Math.max(topLeft.y, botRight.y);
    return new Vector2i[] {
        new Vector2i(minX, minZ),
        new Vector2i(minX, maxZ),
        new Vector2i(maxX, minZ),
        new Vector2i(maxX, maxZ)
    };
  }

  private void assignBounds(Vector2i topLeft, Vector2i botRight) {
    if (topLeft == null || botRight == null) {
      this.topLeft = topLeft;
      this.botRight = botRight;
      return;
    }

    this.topLeft = new Vector2i(
        Math.min(topLeft.x, botRight.x),
        Math.min(topLeft.y, botRight.y));
    this.botRight = new Vector2i(
        Math.max(topLeft.x, botRight.x),
        Math.max(topLeft.y, botRight.y));
  }

  @Override
  public ClaimedAreaComponent clone() {
    ClaimedAreaComponent claimArea = new ClaimedAreaComponent();
    claimArea.topLeft = this.topLeft;
    claimArea.botRight = this.botRight;
    claimArea.y = this.y;
    return claimArea;
  }

  public Vector2i getTopLeft() {
    return topLeft;
  }

  public void setTopLeft(Vector2i topLeft) {
    this.topLeft = topLeft;
  }

  public Vector2i getBotRight() {
    return botRight;
  }

  public void setBotRight(Vector2i botRight) {
    this.botRight = botRight;
  }

  public int getY() {
    return this.y;
  }

  public ComponentType<ChunkStore, ClaimedAreaComponent> getComponentType() {
    return ProjectCivicCore.get().getClaimedAreaComponent();
  }

  public boolean isInside(Vector2i coord2d) {
    if (this.topLeft == null || this.botRight == null) {
      return false;
    }
    int x = coord2d.x;
    int z = coord2d.y;

    return x >= this.topLeft.x && x <= this.botRight.x && z >= this.topLeft.y &&
        z <= this.botRight.y;
  }

  @Override
  public String toString() {
    return "ClaimedAreaComponent{topLeft=" + this.topLeft + ", botRight=" + this.botRight + "}";
  }
}
