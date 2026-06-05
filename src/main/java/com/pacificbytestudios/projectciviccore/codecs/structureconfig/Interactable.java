package com.pacificbytestudios.projectciviccore.codecs.structureconfig;

import java.util.Objects;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.interfaces.Copyable;

public class Interactable implements Copyable<Interactable> {
  public static final BuilderCodec<Interactable> CODEC = BuilderCodec
      .builder(Interactable.class, Interactable::new)

      .append(new KeyedCodec<>("Id", Codec.UUID_BINARY),
          (obj, value) -> obj.id = value,
          (obj) -> obj.id)
      .add()

      .append(new KeyedCodec<>("InteractablePosition", Vec3f.CODEC),
          (obj, value) -> obj.interactablePosition = value,
          obj -> obj.interactablePosition)
      .add()

      .append(new KeyedCodec<>("DependsOn", Vec3f.CODEC),
          (obj, value) -> obj.dependsOn = value,
          obj -> obj.dependsOn)
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
  private Vec3f interactablePosition;
  private Vec3f dependsOn;
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
    this.interactablePosition = interactablePosition;
    this.invalidateOnUse = invalidateOnUse;
    this.dependsOn = dependsOn;
  }

  public boolean isOccupied() {
    return this.occupiedBy != null;
  }

  public boolean isAvailable() {
    return this.isReady && this.occupiedBy == null;
  }

  public boolean hasDependency() {
    return this.dependsOn != null;
  }

  public boolean dependsOn(Vec3f position) {
    return Objects.equals(this.dependsOn, position);
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
    return interactablePosition;
  }

  public void setInteractablePosition(Vec3f interactablePosition) {
    this.interactablePosition = interactablePosition;
  }

  public Vec3f getDependsOn() {
    return dependsOn;
  }

  public void setDependsOn(Vec3f dependsOn) {
    this.dependsOn = dependsOn;
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
    interactable.interactablePosition = Vec3f.from(this.interactablePosition);
    interactable.dependsOn = Vec3f.from(this.dependsOn);
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
            this.interactablePosition.toString(),
            this.dependsOn.toString(),
            this.occupiedBy.toString(),
            this.invalidateOnUse,
            this.isReady);
  }
}
