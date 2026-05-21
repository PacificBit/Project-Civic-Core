package com.pacificbytestudios.projectciviccore.village.components.house;

import java.util.UUID;

import org.joml.Vector3i;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.utils.VectorUtil;

public class VillageBedComponent implements Component<ChunkStore> {
  public static final BuilderCodec<VillageBedComponent> CODEC = BuilderCodec
      .builder(VillageBedComponent.class, VillageBedComponent::new)

      .append(new KeyedCodec<>("IsTaken", Codec.BOOLEAN),
          (obj, value) -> obj.isTaken = value,
          (obj) -> obj.isTaken)
      .add()

      .append(new KeyedCodec<>("Owner", Codec.UUID_BINARY),
          (obj, value) -> obj.owner = value,
          (obj) -> obj.owner)
      .add()

      .append(new KeyedCodec<>("Position", Vector3iUtil.CODEC),
          (obj, value) -> obj.position = value,
          (obj) -> obj.position)
      .add()

      .append(new KeyedCodec<>("Rotation", Rotation.CODEC),
          (obj, value) -> obj.rotation = value,
          (obj) -> obj.rotation)
      .add()

      .build();

  private boolean isTaken;
  private UUID owner;
  private Vector3i position;
  private Rotation rotation;

  @Override
  public VillageBedComponent clone() {
    VillageBedComponent component = new VillageBedComponent();
    component.isTaken = this.isTaken;
    component.owner = this.owner;
    component.position = VectorUtil.clone(this.position);
    component.rotation = this.rotation;
    return component;
  }

  public boolean isTaken() {
    return isTaken;
  }

  public void setTaken(boolean isTaken) {
    this.isTaken = isTaken;
  }

  public UUID getOwner() {
    return owner;
  }

  public void setOwner(UUID owner) {
    this.owner = owner;
  }

  public Vector3i getPosition() {
    return position;
  }

  public void setPosition(Vector3i position) {
    this.position = position;
  }

  public Rotation getRotation() {
    return rotation;
  }

  public void setRotation(Rotation rotation) {
    this.rotation = rotation;
  }

}
