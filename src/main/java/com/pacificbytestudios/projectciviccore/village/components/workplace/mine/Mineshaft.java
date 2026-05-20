package com.pacificbytestudios.projectciviccore.village.components.workplace.mine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3i;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.Short2ObjectMapCodec;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.pacificbytestudios.projectciviccore.codecs.Vector3iMapCodec;
import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;

import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class Mineshaft {
  public static final BuilderCodec<Mineshaft> CODEC = BuilderCodec
      .builder(Mineshaft.class, Mineshaft::new)

      .append(new KeyedCodec<>(
          "MineshaftLevels",
          new Short2ObjectMapCodec<>(
              MineshaftLevel.CODEC,
              Short2ObjectOpenHashMap::new),
          false),
          (obj, value) -> {
            obj.mineshaftLevels = decodeMineshaftLevels(value);
            obj.galleryIdToMineshaftLevel = rebuildGalleryIdIndex(obj.mineshaftLevels);
          },
          obj -> encodeMineshaftLevels(obj.mineshaftLevels))
      .add()

      .append(new KeyedCodec<>(
          "GalleryOffsets",
          new Vector3iMapCodec<Vector3i, HashMap<Vector3i, Vector3i>>(
              Vector3iUtil.CODEC,
              HashMap::new,
              false),
          false),
          (obj, value) -> obj.galleryOffsets = value == null ? new HashMap<>() : value,
          obj -> obj.galleryOffsets)
      .add()

      .append(new KeyedCodec<>("AnchorPoint", Vector3iUtil.CODEC),
          (obj, value) -> obj.anchorPoint = value,
          obj -> obj.anchorPoint)
      .add()

      .append(new KeyedCodec<>("Rotation", Rotation.CODEC),
          (obj, value) -> obj.rotation = value,
          obj -> obj.rotation)
      .add()

      .append(new KeyedCodec<>("LatestLevel", Codec.SHORT),
          (obj, value) -> obj.latestLevel = value,
          obj -> obj.latestLevel)
      .add()

      .build();

  private Map<Short, MineshaftLevel> mineshaftLevels = new HashMap<>();
  private Map<Vector3i, Vector3i> galleryOffsets = new HashMap<>();
  private Map<UUID, Short> galleryIdToMineshaftLevel = new HashMap<>();
  private Vector3i anchorPoint;
  private Rotation rotation;

  private Short latestLevel = 0;

  public Mineshaft() {
  }

  public Mineshaft(Map<Vector3i, Vector3i> galleryOffsets, Vector3i anchorPoint, Rotation rotation) {
    this.galleryOffsets = galleryOffsets;
    this.anchorPoint = anchorPoint;
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
    Vector3i size3i = StructureConfig.Mine.MineshaftLevelConf.SIZE_3I;
    Vector3i levelAnchorPoint = VectorUtil.clone(this.anchorPoint)
        .sub(new Vector3i(0, size3i.y, 0).mul(++this.latestLevel));

    if (levelAnchorPoint.y < 4) {
      return null;
    }

    Set<UUID> galleriesId = mineshaftLevel.buildGalleries(
        StructureConfig.Mine.MineshaftLevelConf.getMineshaftGalleriesWithPrefabOffset(levelAnchorPoint, rotation));
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
    Mineshaft component = new Mineshaft();
    component.mineshaftLevels = new HashMap<>();

    for (Map.Entry<Short, MineshaftLevel> entry : this.mineshaftLevels.entrySet()) {
      Short key = entry.getKey();
      MineshaftLevel value = entry.getValue();

      if (key == null || value == null) {
        continue;
      }

      component.mineshaftLevels.put(key, value.clone());
    }

    component.galleryIdToMineshaftLevel = rebuildGalleryIdIndex(component.mineshaftLevels);

    component.galleryOffsets = new HashMap<>();
    for (Map.Entry<Vector3i, Vector3i> entry : this.galleryOffsets.entrySet()) {
      Vector3i key = entry.getKey();
      Vector3i value = entry.getValue();

      if (key == null || value == null) {
        continue;
      }

      component.galleryOffsets.put(VectorUtil.clone(key), VectorUtil.clone(value));
    }

    component.anchorPoint = VectorUtil.clone(this.anchorPoint);
    component.rotation = this.rotation;
    component.latestLevel = this.latestLevel;

    return component;
  }

  private static Map<Short, MineshaftLevel> decodeMineshaftLevels(Short2ObjectMap<MineshaftLevel> encoded) {
    Map<Short, MineshaftLevel> decoded = new HashMap<>();

    if (encoded == null) {
      return decoded;
    }

    for (Short2ObjectMap.Entry<MineshaftLevel> entry : encoded.short2ObjectEntrySet()) {
      decoded.put(entry.getShortKey(), entry.getValue());
    }

    return decoded;
  }

  private static Short2ObjectMap<MineshaftLevel> encodeMineshaftLevels(Map<Short, MineshaftLevel> decoded) {
    Short2ObjectMap<MineshaftLevel> encoded = new Short2ObjectOpenHashMap<>();

    if (decoded == null) {
      return encoded;
    }

    for (Map.Entry<Short, MineshaftLevel> entry : decoded.entrySet()) {
      Short key = entry.getKey();
      if (key == null) {
        continue;
      }
      encoded.put(key.shortValue(), entry.getValue());
    }

    return encoded;
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
