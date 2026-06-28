package com.pacificbytestudios.projectciviccore.village.components.workplace.mine;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import lombok.Setter;
import lombok.Getter;

@SerializableClass
public class MineshaftGallery {
  public static final short MAX_GLALLERY_SECTION_COUNT = 5;
  public static final BuilderCodec<MineshaftGallery> CODEC = MineshaftGalleryCodec.CODEC;

  UUID galleryId;
  @Getter
  @Setter
  Vec3i cardinalDirection;
  @Getter
  @Setter
  Vec3i startPosition;
  @Setter
  Vec3i lastDiggedPosition;

  @Getter
  @SerialName("TrackedPrefabs")
  List<TrackedPrefabPlacementComponent> restoreSnapshot = new ArrayList<>();

  @Getter
  @Setter
  boolean completed = false;

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

  public void setGalleryId(UUID galleryId) {
    this.galleryId = galleryId == null ? UUID.randomUUID() : galleryId;
  }

  public Vec3i getLastDiggedPosition() {
    if (this.lastDiggedPosition == null) {
      this.lastDiggedPosition = this.startPosition.clone();
    }
    return this.lastDiggedPosition;
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
    return MineshaftGalleryCodec.clone(this);
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
