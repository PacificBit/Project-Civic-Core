package com.pacificbytestudios.projectciviccore.village.components.workplace.mine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3i;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.VectorUtil;

public class MineshaftLevel {
  public static final BuilderCodec<MineshaftLevel> CODEC = BuilderCodec
      .builder(MineshaftLevel.class, MineshaftLevel::new)

      .append(new KeyedCodec<>(
          "Galleries",
          new MapCodec<MineshaftGallery, HashMap<String, MineshaftGallery>>(
              MineshaftGallery.CODEC,
              HashMap::new,
              false),
          false),
          (obj, value) -> obj.galleries = decodeGalleries(value),
          obj -> encodeGalleries(obj.galleries))
      .add()

      .append(new KeyedCodec<>("TrackedPrefab", TrackedPrefabPlacementComponent.CODEC),
          (obj, value) -> obj.trackedPrefab = value,
          (obj) -> obj.trackedPrefab)
      .add()

      .append(new KeyedCodec<>("AnchorPoint", Vector3iUtil.CODEC),
          (obj, value) -> obj.anchorPoint = value,
          (obj) -> obj.anchorPoint)
      .add()

      .build();

  private Map<UUID, MineshaftGallery> galleries = new HashMap<>();
  private TrackedPrefabPlacementComponent trackedPrefab;
  private Vector3i anchorPoint;

  public Map<UUID, MineshaftGallery> getGalleries() {
    return galleries;
  }

  public Set<UUID> buildGalleries(Map<Vector3i, Vector3i> galleryStartPosition) {
    Set<UUID> galleriesIds = new HashSet<>();
    for (Map.Entry<Vector3i, Vector3i> gallery : galleryStartPosition.entrySet()) {
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

  public Vector3i getAnchorPoint() {
    return anchorPoint;
  }

  public void setAnchorPoint(Vector3i anchorPoint) {
    this.anchorPoint = anchorPoint;
  }

  public MineshaftLevel clone() {
    MineshaftLevel component = new MineshaftLevel();
    component.galleries = new HashMap<>();

    for (Map.Entry<UUID, MineshaftGallery> entry : this.galleries.entrySet()) {
      if (entry.getKey() == null || entry.getValue() == null) {
        continue;
      }
      MineshaftGallery clonedGallery = entry.getValue().clone();
      clonedGallery.setGalleryId(entry.getKey());
      component.galleries.put(entry.getKey(), clonedGallery);
    }

    component.trackedPrefab = this.trackedPrefab == null ? null : this.trackedPrefab.clone();
    component.anchorPoint = VectorUtil.clone(this.anchorPoint);

    return component;
  }

  private static Map<UUID, MineshaftGallery> decodeGalleries(Map<String, MineshaftGallery> encoded) {
    Map<UUID, MineshaftGallery> decoded = new HashMap<>();

    if (encoded == null) {
      return decoded;
    }

    encoded.forEach((rawGalleryId, gallery) -> {
      if (gallery == null) {
        return;
      }

      UUID galleryId = parseGalleryId(rawGalleryId, gallery);
      gallery.setGalleryId(galleryId);
      decoded.put(galleryId, gallery);
    });

    return decoded;
  }

  private static HashMap<String, MineshaftGallery> encodeGalleries(Map<UUID, MineshaftGallery> decoded) {
    HashMap<String, MineshaftGallery> encoded = new HashMap<>();

    if (decoded == null) {
      return encoded;
    }

    decoded.forEach((galleryId, gallery) -> {
      if (gallery == null) {
        return;
      }

      UUID normalizedGalleryId = galleryId == null ? gallery.getGalleryId() : galleryId;
      gallery.setGalleryId(normalizedGalleryId);
      encoded.put(normalizedGalleryId.toString(), gallery);
    });

    return encoded;
  }

  private static UUID parseGalleryId(String rawGalleryId, MineshaftGallery gallery) {
    if (rawGalleryId == null || rawGalleryId.isBlank()) {
      return gallery.getGalleryId();
    }

    try {
      return UUID.fromString(rawGalleryId);
    } catch (IllegalArgumentException ignored) {
      return gallery.getGalleryId();
    }
  }
}
