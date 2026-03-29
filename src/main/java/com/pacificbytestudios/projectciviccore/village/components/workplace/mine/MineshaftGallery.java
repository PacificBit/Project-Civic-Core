package com.pacificbytestudios.projectciviccore.village.components.workplace.mine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.math.vector.Vector3i;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;

public class MineshaftGallery {
  public static final short MAX_GLALLERY_SECTION_COUNT = 5;

  public static final BuilderCodec<MineshaftGallery> CODEC = BuilderCodec
      .builder(MineshaftGallery.class, MineshaftGallery::new)

      .append(new KeyedCodec<>("GalleryId", Codec.UUID_BINARY),
          (obj, value) -> obj.setGalleryId(value),
          obj -> obj.getGalleryId())
      .add()

      .append(new KeyedCodec<>("CardinalDirection", Vector3i.CODEC, false),
          (obj, value) -> obj.cardinalDirection = value,
          obj -> obj.cardinalDirection)
      .add()

      .append(new KeyedCodec<>("StartPosition", Vector3i.CODEC),
          (obj, value) -> obj.startPosition = value,
          obj -> obj.startPosition)
      .add()

      .append(new KeyedCodec<>("LastDiggedPosition", Vector3i.CODEC, false),
          (obj, value) -> obj.lastDiggedPosition = value,
          obj -> obj.lastDiggedPosition)
      .add()

      .append(new KeyedCodec<>(
          "TrackedPrefabs",
          new ArrayCodec<>(TrackedPrefabPlacementComponent.CODEC, TrackedPrefabPlacementComponent[]::new),
          false),
          (obj, value) -> obj.restoreSnapshot = value == null
              ? new ArrayList<>()
              : new ArrayList<>(Arrays.asList(value)),
          obj -> obj.restoreSnapshot == null
              ? new TrackedPrefabPlacementComponent[0]
              : obj.restoreSnapshot.toArray(TrackedPrefabPlacementComponent[]::new))
      .add()

      .append(new KeyedCodec<>("Completed", Codec.BOOLEAN),
          (obj, value) -> obj.completed = value,
          obj -> obj.completed)
      .add()

      .build();

  private UUID galleryId;
  private Vector3i cardinalDirection;
  private Vector3i startPosition;
  private Vector3i lastDiggedPosition;
  private List<TrackedPrefabPlacementComponent> restoreSnapshot = new ArrayList<>();
  private boolean completed = false;

  public MineshaftGallery() {
    this.galleryId = UUID.randomUUID();
  }

  public MineshaftGallery(Vector3i cardinalDirection, Vector3i startPosition) {
    this();
    this.startPosition = startPosition == null ? null : startPosition.clone();
    this.lastDiggedPosition = startPosition == null ? null : startPosition.clone();
    this.cardinalDirection = cardinalDirection == null ? null : cardinalDirection.clone();
  }

  public UUID getGalleryId() {
    if (this.galleryId == null) {
      this.galleryId = UUID.randomUUID();
    }
    return galleryId;
  }

  public Vector3i getCardinalDirection() {
    return cardinalDirection;
  }

  public void setCardinalDirection(Vector3i cardinalDirection) {
    this.cardinalDirection = cardinalDirection;
  }

  public void setGalleryId(UUID galleryId) {
    this.galleryId = galleryId == null ? UUID.randomUUID() : galleryId;
  }

  public boolean isCompleted() {
    return completed;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  public Vector3i getLastDiggedPosition() {
    if (this.lastDiggedPosition == null) {
      this.lastDiggedPosition = this.startPosition.clone();
    }
    return this.lastDiggedPosition;
  }

  public Vector3i getStartPosition() {
    return startPosition;
  }

  public void setStartPosition(Vector3i startPosition) {
    this.startPosition = startPosition;
  }

  public void setLastDiggedPosition(Vector3i lastDiggedPosition) {
    this.lastDiggedPosition = lastDiggedPosition;
  }

  public List<TrackedPrefabPlacementComponent> getRestoreSnapshot() {
    return restoreSnapshot;
  }

  public void addRestoreSnapshot(TrackedPrefabPlacementComponent restoreSnapshot) {
    this.restoreSnapshot.add(restoreSnapshot);
  }

  public boolean hasProgress() {
    return !this.restoreSnapshot.isEmpty()
        || (this.lastDiggedPosition != null
            && this.startPosition != null
            && !this.lastDiggedPosition.equals(this.startPosition));
  }

  public void checkIfGalleryIsCompleted() {
    if (this.restoreSnapshot.size() >= MAX_GLALLERY_SECTION_COUNT) {
      this.setCompleted(true);
    }
  }

  public MineshaftGallery clone() {
    MineshaftGallery component = new MineshaftGallery();
    component.galleryId = this.getGalleryId();
    component.cardinalDirection = this.cardinalDirection == null ? null : this.cardinalDirection.clone();
    component.startPosition = this.startPosition == null ? null : this.startPosition.clone();
    component.lastDiggedPosition = this.lastDiggedPosition == null ? null : this.lastDiggedPosition.clone();

    component.restoreSnapshot = new ArrayList<>();
    for (TrackedPrefabPlacementComponent trackedPrefab : this.restoreSnapshot) {
      component.restoreSnapshot.add(trackedPrefab == null ? null : trackedPrefab.clone());
    }

    component.completed = this.completed;
    return component;
  }

}
