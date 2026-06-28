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
import lombok.Getter;
import lombok.Setter;

@SerializableClass
public class MineshaftLevel {
  public static final BuilderCodec<MineshaftLevel> CODEC = MineshaftLevelCodec.CODEC;

  @Getter
  Map<UUID, MineshaftGallery> galleries = new HashMap<>();
  @Getter
  @Setter
  TrackedPrefabPlacementComponent trackedPrefab;
  @Getter
  @Setter
  Vec3i anchorPoint;

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

  public MineshaftLevel clone() {
    return MineshaftLevelCodec.clone(this);
  }
}
