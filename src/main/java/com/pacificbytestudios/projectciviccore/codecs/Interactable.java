package com.pacificbytestudios.projectciviccore.codecs;

import java.util.Objects;
import java.util.UUID;

import org.joml.Vector3i;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3iUtil;

public class Interactable {
  public static final BuilderCodec<Interactable> CODEC = BuilderCodec
      .builder(Interactable.class, Interactable::new)

      .append(new KeyedCodec<>("InteractablePosition", Vector3iUtil.CODEC),
          (obj, value) -> obj.interactablePosition = value,
          obj -> obj.interactablePosition)
      .add()

      .append(new KeyedCodec<>("DependsOn", Vector3iUtil.CODEC),
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

  private Vector3i interactablePosition;
  private Vector3i dependsOn;
  private UUID occupiedBy;
  private boolean invalidateOnUse;
  private boolean isReady = false;

  public Interactable() {
  }

  public Interactable(Vector3i interactablePosition, boolean invalidateOnUse) {
    this(interactablePosition, null, invalidateOnUse);
  }

  public Interactable(Vector3i interactablePosition, Vector3i dependsOn, boolean invalidateOnUse) {
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

  public boolean dependsOn(Vector3i position) {
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

  public Vector3i getInteractablePosition() {
    return interactablePosition;
  }

  public void setInteractablePosition(Vector3i interactablePosition) {
    this.interactablePosition = interactablePosition;
  }

  public Vector3i getDependsOn() {
    return dependsOn;
  }

  public void setDependsOn(Vector3i dependsOn) {
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
    interactable.interactablePosition = this.interactablePosition;
    interactable.dependsOn = this.dependsOn;
    interactable.occupiedBy = this.occupiedBy;
    interactable.invalidateOnUse = this.invalidateOnUse;
    interactable.isReady = this.isReady;
    return interactable;
  }

  @Override
  public String toString() {
    return "Interactable{\n"
        + "  interactablePosition=" + interactablePosition + ",\n"
        + "  dependsOn=" + dependsOn + ",\n"
        + "  occupiedBy=" + occupiedBy + ",\n"
        + "  invalidateOnUse=" + invalidateOnUse + ",\n"
        + "  isReady=" + isReady + "\n"
        + "}";
  }
}
