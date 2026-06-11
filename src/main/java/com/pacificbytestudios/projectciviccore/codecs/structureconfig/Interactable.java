package com.pacificbytestudios.projectciviccore.codecs.structureconfig;

import java.util.Objects;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.interfaces.Copyable;

public class Interactable implements Copyable<Interactable> {
  public static final BuilderCodec<Interactable> CODEC = BuilderCodec
      .builder(Interactable.class, Interactable::new)

      .append(new KeyedCodec<>("Id", Codec.UUID_BINARY),
          (obj, value) -> obj.id = value,
          (obj) -> obj.id)
      .add()

      .append(new KeyedCodec<>("InteractableTransform", BlockTransform.VEC3F_CODEC),
          (obj, value) -> obj.interactableTransform = value,
          obj -> obj.interactableTransform)
      .add()

      .append(new KeyedCodec<>("DependsOnTransform", BlockTransform.VEC3F_CODEC, false),
          (obj, value) -> obj.dependsOnTransform = value,
          obj -> obj.dependsOnTransform)
      .add()

      .append(new KeyedCodec<>("OccupiedBy", Codec.UUID_BINARY),
          (obj, value) -> obj.occupiedBy = value,
          obj -> obj.occupiedBy)
      .add()

      .append(new KeyedCodec<>("InvalidateOnUse", Codec.BOOLEAN),
          (obj, value) -> obj.invalidateOnUse = value,
          obj -> obj.invalidateOnUse)
      .add()

      .append(new KeyedCodec<>("IsReady", Codec.BOOLEAN),
          (obj, value) -> obj.isReady = value,
          obj -> obj.isReady)
      .add()

      .build();

  private UUID id;
  private BlockTransform<Vec3f> interactableTransform;
  private BlockTransform<Vec3f> dependsOnTransform;
  private UUID occupiedBy;
  private boolean invalidateOnUse;
  private boolean isReady = false;

  public Interactable() {
  }

  public Interactable(Vec3f interactablePosition, boolean invalidateOnUse) {
    this(interactablePosition, null, invalidateOnUse);
  }

  public Interactable(Vec3f interactablePosition, Vec3f dependsOn, boolean invalidateOnUse) {
    this(UUID.randomUUID(), interactablePosition, dependsOn, invalidateOnUse);
  }

  public Interactable(UUID id, Vec3f interactablePosition, Vec3f dependsOn, boolean invalidateOnUse) {
    this.id = id;
    this.interactableTransform = new BlockTransform<>(interactablePosition);
    this.invalidateOnUse = invalidateOnUse;
    this.dependsOnTransform = BlockTransform.from(dependsOn);
  }

  public boolean isOccupied() {
    return this.occupiedBy != null;
  }

  public boolean isAvailable() {
    return this.isReady && this.occupiedBy == null;
  }

  public boolean hasDependency() {
    return getDependsOn() != null;
  }

  public boolean dependsOn(Vec3f position) {
    return Objects.equals(getDependsOn(), position);
  }

  public void occupy(UUID villagerId) {
    this.occupiedBy = villagerId;
  }

  public void release() {
    this.occupiedBy = null;
  }

  public void invalidate() {
    this.isReady = false;
  }

  public Vec3f getInteractablePosition() {
    return this.interactableTransform.getPosition();
  }

  public void setInteractablePosition(Vec3f interactablePosition) {
    this.interactableTransform.setPosition(interactablePosition);
  }

  public BlockTransform<Vec3f> getInteractableTransform() {
    return this.interactableTransform;
  }

  public void setInteractableTransform(BlockTransform<Vec3f> interactableTransform) {
    this.interactableTransform = interactableTransform;
  }

  public Vec3f getDependsOn() {
    return this.dependsOnTransform == null ? null : this.dependsOnTransform.getPosition();
  }

  public void setDependsOn(Vec3f dependsOn) {
    this.dependsOnTransform.setPosition(dependsOn);
  }

  public BlockTransform<Vec3f> getDependsOnTransform() {
    return this.dependsOnTransform;
  }

  public void setDependsOnTransform(BlockTransform<Vec3f> dependsOnTransform) {
    this.dependsOnTransform = dependsOnTransform;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getOccupiedBy() {
    return occupiedBy;
  }

  public void setOccupiedBy(UUID occupiedBy) {
    this.occupiedBy = occupiedBy;
  }

  public boolean isInvalidateOnUse() {
    return invalidateOnUse;
  }

  public void setInvalidateOnUse(boolean invalidateOnUse) {
    this.invalidateOnUse = invalidateOnUse;
  }

  public boolean isReady() {
    return isReady;
  }

  public void setReady(boolean isReady) {
    this.isReady = isReady;
  }

  @Override
  public Interactable clone() {
    Interactable interactable = new Interactable();
    interactable.id = this.id;
    interactable.interactableTransform = BlockTransform.clone(this.interactableTransform);
    interactable.dependsOnTransform = BlockTransform.clone(this.dependsOnTransform);
    interactable.occupiedBy = this.occupiedBy;
    interactable.invalidateOnUse = this.invalidateOnUse;
    interactable.isReady = this.isReady;
    return interactable;
  }

  @Override
  public String toString() {
    return ("Interactable: {\n\tId: %s,\n\tInteractablePosition: %s,\n\tDependsOn: %s," +
        "\n\tOccupiedBy: %s,\n\tInvalidateOnUse: %b,\n\tIsReady: %b\n\t}")
        .formatted(
            this.id,
            getInteractablePosition(),
            getDependsOn(),
            this.occupiedBy,
            this.invalidateOnUse,
            this.isReady);
  }

}
