package com.pacificbytestudios.projectciviccore.village.components.workplace.mine;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;

public class MineshaftGallery {
  public static final short MAX_GLALLERY_SECTION_COUNT = 5;

  public static final BuilderCodec<MineshaftGallery> CODEC = BuilderCodec
      .builder(MineshaftGallery.class, MineshaftGallery::new)

      .append(new KeyedCodec<>("GalleryId", Codec.UUID_BINARY),
          (obj, value) -> obj.setGalleryId(value),
          obj -> obj.getGalleryId())
      .add()

      .append(new KeyedCodec<>("CardinalDirection", Vec3i.CODEC, false),
          (obj, value) -> obj.cardinalDirection = value,
          obj -> obj.cardinalDirection)
      .add()

      .append(new KeyedCodec<>("StartPosition", Vec3i.CODEC),
          (obj, value) -> obj.startPosition = value,
          obj -> obj.startPosition)
      .add()

      .append(new KeyedCodec<>("LastDiggedPosition", Vec3i.CODEC, false),
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
  private Vec3i cardinalDirection;
  private Vec3i startPosition;
  private Vec3i lastDiggedPosition;
  private List<TrackedPrefabPlacementComponent> restoreSnapshot = new ArrayList<>();
  private boolean completed = false;

  public MineshaftGallery() {
    this.galleryId = UUID.randomUUID();
  }

  public MineshaftGallery(Vec3i cardinalDirection, Vec3i startPosition) {
    this();
    this.startPosition = startPosition.clone();
    this.lastDiggedPosition = startPosition.clone();
    this.cardinalDirection = cardinalDirection.clone();
  }

  public UUID getGalleryId() {
    if (this.galleryId == null) {
      this.galleryId = UUID.randomUUID();
    }
    return galleryId;
  }

  public Vec3i getCardinalDirection() {
    return cardinalDirection;
  }

  public void setCardinalDirection(Vec3i cardinalDirection) {
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

  public Vec3i getLastDiggedPosition() {
    if (this.lastDiggedPosition == null) {
      this.lastDiggedPosition = this.startPosition.clone();
    }
    return this.lastDiggedPosition;
  }

  public Vec3i getStartPosition() {
    return startPosition;
  }

  public void setStartPosition(Vec3i startPosition) {
    this.startPosition = startPosition;
  }

  public void setLastDiggedPosition(Vec3i lastDiggedPosition) {
    this.lastDiggedPosition = lastDiggedPosition;
  }

  public List<TrackedPrefabPlacementComponent> getRestoreSnapshot() {
    return restoreSnapshot;
  }

  public boolean hasRestoreSnapshotAt(Vec3i sectionOrigin) {
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
    List<Vec3i> seenOrigins = new ArrayList<>();

    for (TrackedPrefabPlacementComponent trackedPrefab : this.restoreSnapshot) {
      if (trackedPrefab == null) {
        continue;
      }

      Vec3i origin = trackedPrefab.getOrigin();
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
    component.cardinalDirection = Vec3i.from(this.cardinalDirection);
    component.startPosition = Vec3i.from(this.startPosition);
    component.lastDiggedPosition = Vec3i.from(this.lastDiggedPosition);

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
      Vec3i sectionOrigin) {
    if (trackedPrefabs == null || sectionOrigin == null) {
      return false;
    }

    for (TrackedPrefabPlacementComponent trackedPrefab : trackedPrefabs) {
      if (trackedPrefab == null) {
        continue;
      }

      Vec3i trackedOrigin = trackedPrefab.getOrigin();
      if (sectionOrigin.equals(trackedOrigin)) {
        return true;
      }
    }

    return false;
  }

  private static boolean containsOrigin(List<Vec3i> seenOrigins, Vec3i candidate) {
    if (seenOrigins == null || candidate == null) {
      return false;
    }

    for (Vec3i seenOrigin : seenOrigins) {
      if (candidate.equals(seenOrigin)) {
        return true;
      }
    }

    return false;
  }

}
