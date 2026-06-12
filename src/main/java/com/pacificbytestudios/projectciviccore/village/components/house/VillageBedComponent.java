package com.pacificbytestudios.projectciviccore.village.components.house;

import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

@SerializableClass
public class VillageBedComponent implements Component<ChunkStore> {
  public static final BuilderCodec<VillageBedComponent> CODEC = VillageBedComponentCodec.CODEC;

  boolean isTaken;
  UUID owner;
  BlockTransform<Vec3i> bedTransform;

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

  public Vec3i getPosition() {
    return this.bedTransform.getPosition();
  }

  public void setPosition(Vec3i position) {
    this.bedTransform = new BlockTransform<>(position);
  }

  public Rotation getRotation() {
    return this.bedTransform.getRotation();
  }

  public void setRotation(Rotation rotation) {
    this.bedTransform.setRotation(rotation);
  }

  public BlockTransform<Vec3i> getBedTransform() {
    return this.bedTransform;
  }

  public void setBedTransform(BlockTransform<Vec3i> bedTransform) {
    this.bedTransform = bedTransform;
  }

  @Override
  public VillageBedComponent clone() {
    return VillageBedComponentCodec.clone(this);
  }
}
