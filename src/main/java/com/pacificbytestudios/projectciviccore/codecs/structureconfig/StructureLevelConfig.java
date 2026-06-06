package com.pacificbytestudios.projectciviccore.codecs.structureconfig;

import java.util.HashMap;
import java.util.Map;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.Short2ObjectMapCodec;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform;
import com.pacificbytestudios.projectciviccore.interfaces.Copyable;
import com.pacificbytestudios.projectciviccore.utils.Utils;

import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class StructureLevelConfig implements Copyable<StructureLevelConfig> {
  public static final BuilderCodec<StructureLevelConfig> CODEC = BuilderCodec
      .builder(StructureLevelConfig.class, StructureLevelConfig::new)

      .append(new KeyedCodec<>("CoreBlocks",
          new Short2ObjectMapCodec<>(BlockTransform.CODEC, Short2ObjectOpenHashMap::new)),
          (obj, value) -> obj.coreBlock = value,
          (obj) -> encodeMap(obj.coreBlock))
      .add()

      .append(new KeyedCodec<>("ConstructionBlock",
          new Short2ObjectMapCodec<>(BlockTransform.CODEC, Short2ObjectOpenHashMap::new)),
          (obj, value) -> obj.constructionBlock = value,
          (obj) -> encodeMap(obj.constructionBlock))
      .add()

      .append(new KeyedCodec<>("PrefabMap",
          new Short2ObjectMapCodec<>(
              new Short2ObjectMapCodec<>(
                  Codec.STRING,
                  Short2ObjectOpenHashMap::new),
              Short2ObjectOpenHashMap::new),
          false),
          (obj, value) -> obj.prefabPathByLevelAndStage = decodePrefabMap(value),
          (obj) -> encodePrefabMap(obj.prefabPathByLevelAndStage))
      .add()

      .append(new KeyedCodec<>("Containers",
          new Short2ObjectMapCodec<>(
              new ArrayCodec<>(
                  BlockTransform.CODEC,
                  BlockTransform[]::new),
              Short2ObjectOpenHashMap::new),
          false),
          (obj, value) -> obj.containers = decodeMap(value),
          (obj) -> encodeMapArray(obj.containers))
      .add()

      .append(new KeyedCodec<>("Interactables",
          new Short2ObjectMapCodec<>(
              new ArrayCodec<>(
                  Interactable.CODEC,
                  Interactable[]::new),
              Short2ObjectOpenHashMap::new),
          false),
          (obj, value) -> obj.interactables = decodeMap(value),
          (obj) -> encodeMapArray(obj.interactables))
      .add()

      .build();

  private Map<Short, BlockTransform> coreBlock;
  private Map<Short, BlockTransform> constructionBlock;
  private Map<Short, Map<Short, String>> prefabPathByLevelAndStage = new HashMap<>();
  private Map<Short, BlockTransform[]> containers = new HashMap<>();
  private Map<Short, Interactable[]> interactables = new HashMap<>();

  public BlockTransform getCoreBlockFor(short level) {
    return this.coreBlock.get(level);
  }

  public BlockTransform getConstructionBlock(short level) {
    return this.constructionBlock.get(level);
  }

  public BlockTransform[] getContainersFor(short level) {
    return this.containers.getOrDefault(level, new BlockTransform[0]);
  }

  public Interactable[] getInteractablesFor(short level) {
    return this.interactables.getOrDefault(level, new Interactable[0]);
  }

  public String getPrefabFor(short level, short stage) {
    Map<Short, String> prefabPathByStage = this.prefabPathByLevelAndStage.get(level);
    return prefabPathByStage == null ? null : prefabPathByStage.get(stage);
  }

  public StructureLevelConfig clone() {
    StructureLevelConfig config = new StructureLevelConfig();
    config.coreBlock = Utils.copyMap(this.coreBlock);
    config.constructionBlock = Utils.copyMap(this.constructionBlock);
    config.prefabPathByLevelAndStage = copyPrefabMap(this.prefabPathByLevelAndStage);
    config.containers = Utils.copyMapArray(this.containers);
    config.interactables = Utils.copyMapArray(this.interactables);
    return config;
  }

  private static <T> Map<Short, T[]> decodeMap(Short2ObjectMap<T[]> encoded) {
    Map<Short, T[]> decoded = new HashMap<>();

    if (encoded == null) {
      return decoded;
    }

    for (Short2ObjectMap.Entry<T[]> entry : encoded.short2ObjectEntrySet()) {
      decoded.put(entry.getShortKey(), entry.getValue());
    }

    return decoded;
  }

  private static <T> Short2ObjectMap<T[]> encodeMapArray(Map<Short, T[]> decoded) {
    Short2ObjectMap<T[]> encoded = new Short2ObjectOpenHashMap<>();

    if (decoded == null) {
      return encoded;
    }

    decoded.forEach((level, values) -> {
      if (level != null && values != null) {
        encoded.put(level.shortValue(), values);
      }
    });
    return encoded;
  }

  private static Map<Short, Map<Short, String>> decodePrefabMap(
      Short2ObjectMap<Short2ObjectMap<String>> encoded) {
    Map<Short, Map<Short, String>> decoded = new HashMap<>();

    if (encoded == null) {
      return decoded;
    }

    for (Short2ObjectMap.Entry<Short2ObjectMap<String>> entry : encoded.short2ObjectEntrySet()) {
      decoded.put(entry.getShortKey(), decodeStringMap(entry.getValue()));
    }

    return decoded;
  }

  private static Map<Short, String> decodeStringMap(Short2ObjectMap<String> encoded) {
    Map<Short, String> decoded = new HashMap<>();

    if (encoded == null) {
      return decoded;
    }

    for (Short2ObjectMap.Entry<String> entry : encoded.short2ObjectEntrySet()) {
      decoded.put(entry.getShortKey(), entry.getValue());
    }

    return decoded;
  }

  private static Short2ObjectMap<Short2ObjectMap<String>> encodePrefabMap(
      Map<Short, Map<Short, String>> decoded) {
    Short2ObjectMap<Short2ObjectMap<String>> encoded = new Short2ObjectOpenHashMap<>();

    if (decoded == null) {
      return encoded;
    }

    decoded.forEach((level, prefabPathByStage) -> {
      if (level != null && prefabPathByStage != null) {
        encoded.put(level.shortValue(), encodeStringMap(prefabPathByStage));
      }
    });

    return encoded;
  }

  private static Short2ObjectMap<String> encodeStringMap(Map<Short, String> decoded) {
    Short2ObjectMap<String> encoded = new Short2ObjectOpenHashMap<>();

    if (decoded == null) {
      return encoded;
    }

    decoded.forEach((stage, prefabPath) -> {
      if (stage != null && prefabPath != null) {
        encoded.put(stage.shortValue(), prefabPath);
      }
    });

    return encoded;
  }

  private static Map<Short, Map<Short, String>> copyPrefabMap(
      Map<Short, Map<Short, String>> source) {
    Map<Short, Map<Short, String>> copy = new HashMap<>();

    if (source == null) {
      return copy;
    }

    source.forEach((level, prefabPathByStage) -> {
      copy.put(level, prefabPathByStage == null ? null : new HashMap<>(prefabPathByStage));
    });

    return copy;
  }

  private static <T extends Copyable<T>> Short2ObjectMap<T> encodeMap(Map<Short, T> decoded) {
    Short2ObjectMap<T> encoded = new Short2ObjectOpenHashMap<>();

    if (decoded == null) {
      return encoded;
    }

    decoded.forEach((level, value) -> {
      if (level != null && value != null) {
        encoded.put(level.shortValue(), value.clone());
      }
    });

    return encoded;
  }

}
