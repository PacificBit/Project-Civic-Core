package com.pacificbytestudios.projectciviccore.codecs;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
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
    this.topLeft = topLeft;
    this.botRight = botRight;
  }

  public ClaimedAreaComponent(ClaimedAreaComponent offset, Vector3i center) {
    int topX = center.getX() + offset.topLeft.x;
    int topZ = center.getZ() + offset.topLeft.y;
    this.topLeft = new Vector2i(topX, topZ);

    int botX = center.getX() + offset.botRight.x;
    int botZ = center.getZ() + offset.botRight.y;
    this.botRight = new Vector2i(botX, botZ);

    this.y = center.y;
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
