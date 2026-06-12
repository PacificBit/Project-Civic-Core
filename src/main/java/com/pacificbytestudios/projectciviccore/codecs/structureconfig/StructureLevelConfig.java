package com.pacificbytestudios.projectciviccore.codecs.structureconfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform3D;
import com.pacificbytestudios.projectciviccore.interfaces.Copyable;

@SerializableClass
public class StructureLevelConfig implements Copyable<StructureLevelConfig> {
  public static final BuilderCodec<StructureLevelConfig> CODEC = StructureLevelConfigCodec.CODEC;

  @SerialName("CoreBlocks")
  Map<Short, BlockTransform3D> coreBlock = new HashMap<>();

  @SerialName("PrefabMap")
  Map<Short, Map<Short, String>> prefabPathByLevelAndStage = new HashMap<>();

  Map<Short, BlockTransform3D> constructionBlock = new HashMap<>();
  Map<Short, BlockTransform3D[]> containers = new HashMap<>();
  Map<Short, Interactable[]> interactables = new HashMap<>();

  public BlockTransform3D getCoreBlockFor(short level) {
    return this.coreBlock.get(level);
  }

  public BlockTransform3D getConstructionBlock(short level) {
    return this.constructionBlock.get(level);
  }

  @SuppressWarnings("unchecked")
  public BlockTransform3D[] getContainersFor(short level) {
    return this.containers.getOrDefault(
        level,
        new BlockTransform3D[0]);
  }

  public Interactable[] getInteractablesFor(short level) {
    return this.interactables.getOrDefault(level, new Interactable[0]);
  }

  // The codec may assign unmodifiable fastutil maps to these fields, so replace
  // the
  // map with a mutable copy before inserting.
  public void putCoreBlock(short level, BlockTransform3D transform) {
    this.coreBlock = mutableCopy(this.coreBlock);
    this.coreBlock.put(level, transform);
  }

  public void putContainers(short level, BlockTransform3D[] containers) {
    this.containers = mutableCopy(this.containers);
    this.containers.put(level, containers);
  }

  public void putInteractables(short level, Interactable[] interactables) {
    this.interactables = mutableCopy(this.interactables);
    this.interactables.put(level, interactables);
  }

  private static <T> Map<Short, T> mutableCopy(Map<Short, T> source) {
    return source == null ? new HashMap<>() : new HashMap<>(source);
  }

  public String getPrefabFor(short level, short stage) {
    Map<Short, String> prefabPathByStage = this.prefabPathByLevelAndStage.get(level);
    return prefabPathByStage == null ? null : prefabPathByStage.get(stage);
  }

  public Set<Short> getConfiguredLevels() {
    Set<Short> levels = new HashSet<>();
    levels.addAll(this.coreBlock.keySet());
    levels.addAll(this.constructionBlock.keySet());
    levels.addAll(this.prefabPathByLevelAndStage.keySet());
    return Collections.unmodifiableSet(levels);
  }

  @Override
  public StructureLevelConfig clone() {
    return StructureLevelConfigCodec.clone(this);
  }

}
