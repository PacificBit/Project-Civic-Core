package com.pacificbytestudios.projectciviccore.village.codec;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector2i;
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
      .build();

  private Vector2i topLeft = new Vector2i();
  private Vector2i botRight = new Vector2i();

  public ClaimedAreaComponent() {
    this(null, null);
  }

  public ClaimedAreaComponent(Vector2i topLeft, Vector2i botRight) {
    this.topLeft = topLeft;
    this.botRight = botRight;
  }

  @Override
  public ClaimedAreaComponent clone() {
    ClaimedAreaComponent claimArea = new ClaimedAreaComponent();
    claimArea.topLeft = this.topLeft;
    claimArea.botRight = this.botRight;
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

  public ComponentType<ChunkStore, ClaimedAreaComponent> getComponentType() {
    return ProjectCivicCore.get().getClaimedAreaComponent();
  }
}
