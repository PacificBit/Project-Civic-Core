package com.pacificbytestudios.projectciviccore.village.components.workplace.mine;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.joml.Vector3i;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.VectorUtil;

public class MineshaftGallery {
  public static final short MAX_GLALLERY_SECTION_COUNT = 5;

  public static final BuilderCodec<MineshaftGallery> CODEC = BuilderCodec
      .builder(MineshaftGallery.class, MineshaftGallery::new)

      .append(new KeyedCodec<>("GalleryId", Codec.UUID_BINARY),
          (obj, value) -> obj.setGalleryId(value),
          obj -> obj.getGalleryId())
      .add()

      .append(new KeyedCodec<>("CardinalDirection", Vector3iUtil.CODEC, false),
          (obj, value) -> obj.cardinalDirection = value,
          obj -> obj.cardinalDirection)
      .add()

      .append(new KeyedCodec<>("StartPosition", Vector3iUtil.CODEC),
          (obj, value) -> obj.startPosition = value,
          obj -> obj.startPosition)
      .add()

      .append(new KeyedCodec<>("LastDiggedPosition", Vector3iUtil.CODEC, false),
          (obj, value) -> obj.lastDiggedPosition = value,
          obj -> obj.lastDiggedPosition)
      .add()

      .append(new KeyedCodec<>(
          "TrackedPrefabs",
          new ArrayCodec<>(TrackedPrefabPlacementComponent.CODEC, TrackedPrefabPlacementComponent[]::new),
          false),
          (obj, value) -> obj.restoreSnapshot = dedupeRestoreSnapshots(value),
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
    this.startPosition = VectorUtil.clone(startPosition);
    this.lastDiggedPosition = VectorUtil.clone(startPosition);
    this.cardinalDirection = VectorUtil.clone(cardinalDirection);
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
      this.lastDiggedPosition = VectorUtil.clone(this.startPosition);
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

  public boolean hasRestoreSnapshotAt(Vector3i sectionOrigin) {
    return containsRestoreSnapshotAt(this.restoreSnapshot, sectionOrigin);
  }

  public boolean addRestoreSnapshot(TrackedPrefabPlacementComponent restoreSnapshot) {
    if (restoreSnapshot == null || this.hasRestoreSnapshotAt(restoreSnapshot.getOrigin())) {
      return false;
    }
    this.restoreSnapshot.add(restoreSnapshot);
    return true;
  }

  public int getBuiltSectionCount() {
    int builtSections = 0;
    List<Vector3i> seenOrigins = new ArrayList<>();

    for (TrackedPrefabPlacementComponent trackedPrefab : this.restoreSnapshot) {
      if (trackedPrefab == null) {
        continue;
      }

      Vector3i origin = trackedPrefab.getOrigin();
      if (containsOrigin(seenOrigins, origin)) {
        continue;
      }

      seenOrigins.add(origin);
      builtSections++;
    }

    return builtSections;
  }

  public boolean hasProgress() {
    return !this.restoreSnapshot.isEmpty()
        || (this.lastDiggedPosition != null
            && this.startPosition != null
            && !this.lastDiggedPosition.equals(this.startPosition));
  }

  public void checkIfGalleryIsCompleted() {
    if (this.getBuiltSectionCount() >= MAX_GLALLERY_SECTION_COUNT) {
      this.setCompleted(true);
    }
  }

  public MineshaftGallery clone() {
    MineshaftGallery component = new MineshaftGallery();
    component.galleryId = this.getGalleryId();
    component.cardinalDirection = VectorUtil.clone(this.cardinalDirection);
    component.startPosition = VectorUtil.clone(this.startPosition);
    component.lastDiggedPosition = VectorUtil.clone(this.lastDiggedPosition);

    component.restoreSnapshot = new ArrayList<>();
    for (TrackedPrefabPlacementComponent trackedPrefab : this.restoreSnapshot) {
      component.restoreSnapshot.add(trackedPrefab == null ? null : trackedPrefab.clone());
    }

    component.completed = this.completed;
    return component;
  }

  private static ArrayList<TrackedPrefabPlacementComponent> dedupeRestoreSnapshots(
      TrackedPrefabPlacementComponent[] trackedPrefabs) {
    ArrayList<TrackedPrefabPlacementComponent> deduped = new ArrayList<>();
    if (trackedPrefabs == null) {
      return deduped;
    }

    for (TrackedPrefabPlacementComponent trackedPrefab : trackedPrefabs) {
      if (trackedPrefab == null || containsRestoreSnapshotAt(deduped, trackedPrefab.getOrigin())) {
        continue;
      }

      deduped.add(trackedPrefab);
    }

    return deduped;
  }

  private static boolean containsRestoreSnapshotAt(
      List<TrackedPrefabPlacementComponent> trackedPrefabs,
      Vector3i sectionOrigin) {
    if (trackedPrefabs == null || sectionOrigin == null) {
      return false;
    }

    for (TrackedPrefabPlacementComponent trackedPrefab : trackedPrefabs) {
      if (trackedPrefab == null) {
        continue;
      }

      Vector3i trackedOrigin = trackedPrefab.getOrigin();
      if (sectionOrigin.equals(trackedOrigin)) {
        return true;
      }
    }

    return false;
  }

  private static boolean containsOrigin(List<Vector3i> seenOrigins, Vector3i candidate) {
    if (seenOrigins == null || candidate == null) {
      return false;
    }

    for (Vector3i seenOrigin : seenOrigins) {
      if (candidate.equals(seenOrigin)) {
        return true;
      }
    }

    return false;
  }

}
