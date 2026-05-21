package com.pacificbytestudios.projectciviccore.village.components.heart;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.EnumMapCodec;
import com.hypixel.hytale.codec.codecs.map.Short2ObjectMapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;

import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class VillageHeartConfigComponent implements Component<ChunkStore> {
  public static final BuilderCodec<VillageHeartConfigComponent> CODEC = BuilderCodec
      .builder(VillageHeartConfigComponent.class, VillageHeartConfigComponent::new)

      .append(new KeyedCodec<>("MaxVillageLevel", Codec.SHORT),
          (obj, value) -> obj.maxLevel = value,
          (obj) -> obj.maxLevel)
      .add()

      .append(new KeyedCodec<>("ClaimRadiusPerLevel", new Short2ObjectMapCodec<>(
          Codec.SHORT,
          Short2ObjectOpenHashMap::new), false),
          (obj, value) -> obj.claimRadiusPerLevel = decodeShortMap(value),
          (obj) -> encodeShortMap(obj.claimRadiusPerLevel))
      .add()

      .append(new KeyedCodec<>("VillagerCapPerLevel", new Short2ObjectMapCodec<>(
          Codec.SHORT,
          Short2ObjectOpenHashMap::new), false),
          (obj, value) -> obj.villagerCapPerLevel = decodeShortMap(value),
          (obj) -> encodeShortMap(obj.villagerCapPerLevel))
      .add()

      .append(new KeyedCodec<>("FallbackMainRoadTurnChance", Codec.SHORT, false),
          (obj, value) -> obj.fallbackMainRoadTurnChance = value,
          (obj) -> obj.fallbackMainRoadTurnChance)
      .add()

      .append(new KeyedCodec<>("PostBuildSideRoadChance", Codec.SHORT, false),
          (obj, value) -> obj.postBuildSideRoadChance = value,
          (obj) -> obj.postBuildSideRoadChance)
      .add()

      .append(new KeyedCodec<>("StructureLimitsPerLevel", new Short2ObjectMapCodec<>(
          new EnumMapCodec<>(
              StructureType.class,
              Codec.SHORT,
              () -> new EnumMap<>(StructureType.class)),
          Short2ObjectOpenHashMap::new), false),
          (obj, value) -> obj.structureCountLimitPerLevel = decodeMap(value),
          (obj) -> encodeMap(obj.structureCountLimitPerLevel))
      .add()

      .append(new KeyedCodec<>("StructureMaxLevelPerVillageLevel", new Short2ObjectMapCodec<>(
          new EnumMapCodec<>(
              StructureType.class,
              Codec.SHORT,
              () -> new EnumMap<>(StructureType.class)),
          Short2ObjectOpenHashMap::new), false),
          (obj, value) -> obj.structureMaxLevelPerVillageLevel = decodeMap(value),
          (obj) -> encodeMap(obj.structureMaxLevelPerVillageLevel))
      .add()

      .build();

  private short maxLevel;
  private Map<Short, Short> claimRadiusPerLevel = new HashMap<>();
  private Map<Short, Short> villagerCapPerLevel = new HashMap<>();
  private short fallbackMainRoadTurnChance;
  private short postBuildSideRoadChance;
  private Map<Short, Map<StructureType, Short>> structureCountLimitPerLevel = new HashMap<>();
  private Map<Short, Map<StructureType, Short>> structureMaxLevelPerVillageLevel = new HashMap<>();

  public short getMaxLevel() {
    return maxLevel;
  }

  public short getClaimRadiusPerLevel(short level) {
    return this.claimRadiusPerLevel.getOrDefault(level, (short) 0);
  }

  public short getVillagerCapPerLevel(short level) {
    return this.villagerCapPerLevel.getOrDefault(level, (short) 0);
  }

  public short getFallbackMainRoadTurnChance() {
    return this.fallbackMainRoadTurnChance;
  }

  public short getPostBuildSideRoadChance() {
    return this.postBuildSideRoadChance;
  }

  public Map<StructureType, Short> getStructureLimitsPerLevel(short level) {
    Map<StructureType, Short> structureLimits = this.structureCountLimitPerLevel.get(level);
    if (structureLimits == null || structureLimits.isEmpty()) {
      return Collections.emptyMap();
    }

    return Collections.unmodifiableMap(structureLimits);
  }

  public Map<StructureType, Short> getStructureMaxLevelPerLevel(short level) {
    Map<StructureType, Short> structureLimits = this.structureMaxLevelPerVillageLevel.get(level);
    if (structureLimits == null || structureLimits.isEmpty()) {
      return Collections.emptyMap();
    }

    return Collections.unmodifiableMap(structureLimits);
  }

  public int getMaxRadius() {
    return getClaimRadiusPerLevel(getMaxLevel());
  }

  @Override
  public VillageHeartConfigComponent clone() {
    VillageHeartConfigComponent component = new VillageHeartConfigComponent();
    component.maxLevel = this.maxLevel;
    component.claimRadiusPerLevel = copyShortMap(this.claimRadiusPerLevel);
    component.villagerCapPerLevel = copyShortMap(this.villagerCapPerLevel);
    component.fallbackMainRoadTurnChance = this.fallbackMainRoadTurnChance;
    component.postBuildSideRoadChance = this.postBuildSideRoadChance;
    component.structureCountLimitPerLevel = deepCopyStructureLimitsPerLevel(this.structureCountLimitPerLevel);
    component.structureMaxLevelPerVillageLevel = deepCopyStructureLimitsPerLevel(
        this.structureMaxLevelPerVillageLevel);
    return component;
  }

  private static Map<Short, Short> decodeShortMap(Short2ObjectMap<Short> encoded) {
    Map<Short, Short> decoded = new HashMap<>();

    if (encoded == null) {
      return decoded;
    }

    encoded.forEach((level, value) -> decoded.put(level, value));
    return decoded;
  }

  private static Short2ObjectMap<Short> encodeShortMap(Map<Short, Short> shortMap) {
    Short2ObjectOpenHashMap<Short> encoded = new Short2ObjectOpenHashMap<>();

    if (shortMap == null) {
      return encoded;
    }

    shortMap.forEach((level, value) -> {
      if (level == null || value == null) {
        return;
      }
      encoded.put(level.shortValue(), value);
    });
    return encoded;
  }

  private static Map<Short, Short> copyShortMap(Map<Short, Short> source) {
    Map<Short, Short> copy = new HashMap<>();

    if (source == null) {
      return copy;
    }

    source.forEach((level, value) -> {
      if (level != null && value != null) {
        copy.put(level, value);
      }
    });
    return copy;
  }

  private static Map<Short, Map<StructureType, Short>> decodeMap(
      Short2ObjectMap<Map<StructureType, Short>> encoded) {
    Map<Short, Map<StructureType, Short>> decoded = new HashMap<>();

    if (encoded == null) {
      return decoded;
    }

    encoded.forEach((level, limits) -> decoded.put(level, copyStructureLimits(limits)));
    return decoded;
  }

  private static Short2ObjectMap<Map<StructureType, Short>> encodeMap(
      Map<Short, Map<StructureType, Short>> structureLimitsPerLevel) {
    Short2ObjectOpenHashMap<Map<StructureType, Short>> encoded = new Short2ObjectOpenHashMap<>();

    if (structureLimitsPerLevel == null) {
      return encoded;
    }

    structureLimitsPerLevel.forEach((level, limits) -> {
      if (level == null) {
        return;
      }
      encoded.put(level.shortValue(), copyStructureLimits(limits));
    });
    return encoded;
  }

  private static Map<Short, Map<StructureType, Short>> deepCopyStructureLimitsPerLevel(
      Map<Short, Map<StructureType, Short>> structureLimitsPerLevel) {
    Map<Short, Map<StructureType, Short>> copy = new HashMap<>();

    if (structureLimitsPerLevel == null) {
      return copy;
    }

    structureLimitsPerLevel.forEach((level, limits) -> {
      if (level != null) {
        copy.put(level, copyStructureLimits(limits));
      }
    });
    return copy;
  }

  private static Map<StructureType, Short> copyStructureLimits(Map<StructureType, Short> structureLimits) {
    Map<StructureType, Short> copy = new EnumMap<>(StructureType.class);

    if (structureLimits == null) {
      return copy;
    }

    copy.putAll(structureLimits);
    return copy;
  }
}
