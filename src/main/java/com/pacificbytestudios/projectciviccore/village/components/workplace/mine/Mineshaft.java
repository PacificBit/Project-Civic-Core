package com.pacificbytestudios.projectciviccore.village.components.workplace.mine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;

@SerializableClass
public class Mineshaft {
  public static final BuilderCodec<Mineshaft> CODEC = MineshaftCodec.CODEC;

  Map<Short, MineshaftLevel> mineshaftLevels = new HashMap<>();
  Map<Vec3i, Vec3i> galleryOffsets = new HashMap<>();
  Map<UUID, Short> galleryIdToMineshaftLevel = new HashMap<>();
  Vec3i anchorPoint;
  Rotation rotation;
  Short latestLevel = 0;

  public Mineshaft() {
  }

  public Mineshaft(Map<Vec3i, Vec3i> galleryOffsets, Vec3i anchorPoint, Rotation rotation) {
    this.galleryOffsets = galleryOffsets;
    this.anchorPoint = Vec3i.from(anchorPoint);
    this.rotation = rotation;
  }

  public Rotation getRotation() {
    return rotation;
  }

  public void setRotation(Rotation rotation) {
    this.rotation = rotation;
  }

  public MineshaftLevel[] getMineshaftLevels() {
    return this.mineshaftLevels.values().toArray(MineshaftLevel[]::new);
  }

  public Short getLevelNumberForGallery(UUID galleryId) {
    if (galleryId == null) {
      return null;
    }

    Short level = this.galleryIdToMineshaftLevel.get(galleryId);
    if (level != null) {
      return level;
    }

    this.galleryIdToMineshaftLevel = rebuildGalleryIdIndex(this.mineshaftLevels);
    return this.galleryIdToMineshaftLevel.get(galleryId);
  }

  public MineshaftGallery getGalleryFromId(UUID galleryId) {
    Short level = this.getLevelNumberForGallery(galleryId);
    if (level == null) {
      return null;
    }

    MineshaftLevel mineshaftLevel = this.mineshaftLevels.get(level);
    if (mineshaftLevel == null) {
      return null;
    }
    return mineshaftLevel.getGalleryFromId(galleryId);
  }

  public boolean markGalleryAsCompleted(UUID galleryId) {
    MineshaftGallery gallery = getGalleryFromId(galleryId);
    if (gallery == null) {
      return true;
    }
    gallery.setCompleted(true);
    return true;
  }

  public MineshaftLevel createNewMineshaftLevel() {
    MineshaftLevel mineshaftLevel = new MineshaftLevel();
    Vec3i size3i = StructureConfig.Mine.MineshaftLevelConf.SIZE_3I;

    Vec3i currentMineshaftLevel = new Vec3i(0, size3i.y, 0);
    Vec3i levelAnchorPoint = Vec3i.from(this.anchorPoint.clone().sub(
        currentMineshaftLevel.mul(++this.latestLevel)));

    if (levelAnchorPoint.y < 4) {
      return null;
    }

    Set<UUID> galleriesId = mineshaftLevel.buildGalleries(
        StructureConfig.Mine.MineshaftLevelConf.getMineshaftGalleriesWithPrefabOffset(
            levelAnchorPoint,
            rotation));
    mineshaftLevel.setAnchorPoint(levelAnchorPoint);

    galleriesId.stream().forEach(galleryId -> galleryIdToMineshaftLevel.put(galleryId, this.latestLevel));

    this.mineshaftLevels.put(this.latestLevel, mineshaftLevel);
    return mineshaftLevel;
  }

  public UUID getFirstAvailableGalleryId() {
    MineshaftLevel mineshaftLevel = this.mineshaftLevels.get(this.latestLevel);

    if (mineshaftLevel == null) {
      return null;
    }

    return mineshaftLevel.getFirstAvailableGalleryId();
  }

  public Mineshaft clone() {
    return MineshaftCodec.clone(this);
  }

  private static Map<UUID, Short> rebuildGalleryIdIndex(Map<Short, MineshaftLevel> mineshaftLevels) {
    Map<UUID, Short> galleryIdToMineshaftLevel = new HashMap<>();

    if (mineshaftLevels == null) {
      return galleryIdToMineshaftLevel;
    }

    for (Map.Entry<Short, MineshaftLevel> levelEntry : mineshaftLevels.entrySet()) {
      Short level = levelEntry.getKey();
      MineshaftLevel mineshaftLevel = levelEntry.getValue();

      if (level == null || mineshaftLevel == null) {
        continue;
      }

      mineshaftLevel.getGalleries().forEach((galleryId, gallery) -> {
        if (gallery == null) {
          return;
        }

        UUID normalizedGalleryId = galleryId == null ? gallery.getGalleryId() : galleryId;
        gallery.setGalleryId(normalizedGalleryId);
        galleryIdToMineshaftLevel.put(normalizedGalleryId, level);
      });
    }

    return galleryIdToMineshaftLevel;
  }
}
