package com.pacificbytestudios.projectciviccore.village.components.workplace.mine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;

@SerializableClass
public class MineshaftLevel {
  public static final BuilderCodec<MineshaftLevel> CODEC = MineshaftLevelCodec.CODEC;

  Map<UUID, MineshaftGallery> galleries = new HashMap<>();
  TrackedPrefabPlacementComponent trackedPrefab;
  Vec3i anchorPoint;

  public Map<UUID, MineshaftGallery> getGalleries() {
    return galleries;
  }

  public Set<UUID> buildGalleries(Map<Vec3i, Vec3i> galleryStartPosition) {
    Set<UUID> galleriesIds = new HashSet<>();
    for (Map.Entry<Vec3i, Vec3i> gallery : galleryStartPosition.entrySet()) {
      MineshaftGallery mineshaftGallery = new MineshaftGallery(gallery.getKey(), gallery.getValue());
      UUID galleryId = mineshaftGallery.getGalleryId();
      this.galleries.put(galleryId, mineshaftGallery);
      galleriesIds.add(galleryId);
    }
    return galleriesIds;
  }

  public UUID getFirstAvailableGalleryId() {
    for (Map.Entry<UUID, MineshaftGallery> gallery : this.galleries.entrySet()) {
      MineshaftGallery value = gallery.getValue();
      if (value != null && !value.isCompleted() && value.hasProgress()) {
        return gallery.getKey();
      }
    }

    for (Map.Entry<UUID, MineshaftGallery> gallery : this.galleries.entrySet()) {
      MineshaftGallery value = gallery.getValue();
      if (value != null && !value.isCompleted()) {
        return gallery.getKey();
      }
    }
    return null;
  }

  public MineshaftGallery getGalleryFromId(UUID galleryId) {
    return this.galleries.get(galleryId);
  }

  public TrackedPrefabPlacementComponent getTrackedPrefab() {
    return trackedPrefab;
  }

  public void setTrackedPrefab(TrackedPrefabPlacementComponent trackedPrefab) {
    this.trackedPrefab = trackedPrefab;
  }

  public Vec3i getAnchorPoint() {
    return anchorPoint;
  }

  public void setAnchorPoint(Vec3i anchorPoint) {
    this.anchorPoint = anchorPoint;
  }

  public MineshaftLevel clone() {
    return MineshaftLevelCodec.clone(this);
  }
}
