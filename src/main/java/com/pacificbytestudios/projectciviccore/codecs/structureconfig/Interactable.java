package com.pacificbytestudios.projectciviccore.codecs.structureconfig;

import java.util.Objects;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform3D;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.interfaces.Copyable;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SerializableClass
public class Interactable implements Copyable<Interactable> {
  public static final BuilderCodec<Interactable> CODEC = InteractableCodec.CODEC;

  UUID id;
  BlockTransform3D interactableTransform;
  BlockTransform3D dependsOnTransform;
  UUID occupiedBy;
  boolean invalidateOnUse;
  boolean isReady = false;

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
    this.interactableTransform = new BlockTransform3D(interactablePosition);
    this.invalidateOnUse = invalidateOnUse;
    this.dependsOnTransform = BlockTransform3D.from(dependsOn);
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
    return this.interactableTransform.getPosition3f();
  }

  public void setInteractablePosition(Vec3f interactablePosition) {
    this.interactableTransform.setPosition(interactablePosition);
  }

  public Vec3f getDependsOn() {
    return this.dependsOnTransform == null ? null : this.dependsOnTransform.getPosition3f();
  }

  public void setDependsOn(Vec3f dependsOn) {
    this.dependsOnTransform.setPosition(dependsOn);
  }

  @Override
  public Interactable clone() {
    return InteractableCodec.clone(this);
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
