package com.pacificbytestudios.projectciviccore.village.components.house;

import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

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

      .append(new KeyedCodec<>("BedTransform", BlockTransform.VEC3I_CODEC),
          (obj, value) -> obj.bedTransform = value,
          (obj) -> obj.bedTransform)
      .add()

      .build();

  private boolean isTaken;
  private UUID owner;
  private BlockTransform<Vec3i> bedTransform;

  @Override
  public VillageBedComponent clone() {
    VillageBedComponent component = new VillageBedComponent();
    component.isTaken = this.isTaken;
    component.owner = this.owner;
    component.bedTransform = BlockTransform.clone(this.bedTransform);
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

}
