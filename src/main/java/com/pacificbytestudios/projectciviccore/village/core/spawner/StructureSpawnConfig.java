package com.pacificbytestudios.projectciviccore.village.core.spawner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.codecs.BlockTransform3D;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.Interactable;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.StructureLevelConfig;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.builder.BuildingSnapshotComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.structures.VillageStructuresConfig;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageObjectiveType;

import io.sentry.util.Pair;

final class StructureSpawnConfig {
  private StructureSpawnConfig() {
  }

  static StructureLevelConfig get(VillageHeartComponent heart, StructureType type) {
    if (heart == null) {
      throw new IllegalArgumentException("Village heart is required for " + type + " structure config");
    }

    return get(heart.getEntityRef(), type);
  }

  static StructureLevelConfig get(EntityRef villageRef, StructureType type) {
    if (villageRef == null) {
      throw new IllegalArgumentException("Village ref is required for " + type + " structure config");
    }

    Ref<ChunkStore> ref = villageRef.getRef();
    if (ref == null || !ref.isValid()) {
      throw new IllegalArgumentException("Village ref is invalid for " + type + " structure config");
    }

    Store<ChunkStore> store = ref.getStore();
    VillageStructuresConfig structuresConfig = store.getComponent(
        ref,
        VillageStructuresConfig.getComponetType());
    if (structuresConfig == null) {
      throw new IllegalStateException("VillageStructuresConfig is missing");
    }

    StructureLevelConfig levelConfig = structuresConfig.getStructureLevelConfigsFor(type);
    if (levelConfig == null) {
      throw new IllegalStateException("StructureLevelConfig is missing for " + type);
    }

    return levelConfig;
  }

  static String getPrefab(StructureLevelConfig config, int level, short stage) {
    String prefab = config.getPrefabFor((short) level, stage);
    if (prefab == null || prefab.isBlank()) {
      throw new IllegalStateException(
          "Prefab is missing for level " + level + " and stage " + stage);
    }
    return prefab;
  }

  static String getCompletedPrefab(StructureLevelConfig config, StructureType type, int level) {
    return getPrefab(config, level, VillageObjectiveType.getStages(type));
  }

  static BlockTransform3D getCoreBlock(StructureLevelConfig config, int level) {
    BlockTransform3D block = config.getCoreBlockFor((short) level);
    if (block == null) {
      throw new IllegalStateException("Core block config is missing for level " + level);
    }
    return block;
  }

  static BlockTransform3D getConstructionBlock(StructureLevelConfig config, int level) {
    BlockTransform3D block = config.getConstructionBlock((short) level);
    if (block == null) {
      throw new IllegalStateException("Construction block config is missing for level " + level);
    }
    return block;
  }

  static BlockTransform3D getFirstContainer(StructureLevelConfig config, int level) {
    BlockTransform3D[] containers = config.getContainersFor((short) level);
    if (containers.length == 0 || containers[0] == null) {
      throw new IllegalStateException("Container config is missing for level " + level);
    }
    return containers[0];
  }

  static int blockRotationIndex(BlockTransform3D block, Rotation structureRotation) {
    Rotation blockRotation = StructurePlacementUtils.addYaw(block.getRotation(), structureRotation);
    return RotationTuple.of(blockRotation, Rotation.None, Rotation.None).index();
  }

  static int constructionSignRotationIndex(BlockTransform3D block, Rotation structureRotation) {
    return blockRotationIndex(block, structureRotation);
  }

  static Vec3i blockPosition(Vec3i prefabAnchor, BlockTransform3D block, Rotation structureRotation) {
    return StructurePlacementUtils.withOffset(
        prefabAnchor,
        block.getPosition3i(),
        structureRotation);
  }

  static Vec3f accessPosition(Vec3i blockPosition, BlockTransform3D block, Rotation structureRotation) {
    return StructurePlacementUtils.withOffset(
        blockPosition,
        block.getAccessOffset3f(),
        structureRotation);
  }

  static Vec3f accessOffset(BlockTransform3D block, Rotation structureRotation) {
    return StructurePlacementUtils.rotatePointOffset(
        block.getAccessOffset3f(),
        structureRotation);
  }

  static Vec3i prefabAnchorFromBlock(Vec3i blockPosition, BlockTransform3D block, Rotation structureRotation) {
    return StructurePlacementUtils.removeOffset(
        blockPosition,
        block.getPosition3i(),
        structureRotation);
  }

  static List<Interactable> absoluteInteractables(
      StructureLevelConfig config,
      Vec3i prefabAnchor,
      Rotation structureRotation,
      int level) {
    Interactable[] template = config.getInteractablesFor((short) level);
    List<Interactable> result = new ArrayList<>(template.length);

    for (Interactable interactable : template) {
      if (interactable == null) {
        continue;
      }

      Interactable copy = interactable.clone();
      copy.setInteractablePosition(StructurePlacementUtils.withOffset(
          prefabAnchor,
          interactable.getInteractablePosition(),
          structureRotation));

      if (interactable.getDependsOn() != null) {
        copy.setDependsOn(StructurePlacementUtils.withOffset(
            prefabAnchor,
            interactable.getDependsOn(),
            structureRotation));
      }

      result.add(copy);
    }

    return result;
  }

  static Pair<WorldChunk, Ref<ChunkStore>> findConstructionSign(
      WorldChunk chunk,
      Vec3i prefabAnchor,
      Rotation structureRotation,
      StructureLevelConfig config,
      StructureType type) {
    return config.getConfiguredLevels().stream()
        .sorted(Comparator.naturalOrder())
        .map(level -> findConstructionSignAtLevel(chunk, prefabAnchor, structureRotation, config, type, level))
        .filter(result -> result != null)
        .findFirst()
        .orElse(null);
  }

  private static Pair<WorldChunk, Ref<ChunkStore>> findConstructionSignAtLevel(
      WorldChunk chunk,
      Vec3i prefabAnchor,
      Rotation structureRotation,
      StructureLevelConfig config,
      StructureType type,
      short level) {
    BlockTransform3D constructionBlock = config.getConstructionBlock(level);
    if (constructionBlock == null) {
      return null;
    }

    Vec3i constructionSignPosition = blockPosition(prefabAnchor, constructionBlock, structureRotation);
    Pair<WorldChunk, Ref<ChunkStore>> result = Utils.getBlockComponentEntityChunkAware(
        chunk,
        constructionSignPosition);
    if (result == null || result.getSecond() == null || !result.getSecond().isValid()) {
      return null;
    }

    BuildingSnapshotComponent snapshot = result.getSecond().getStore().getComponent(
        result.getSecond(),
        BuildingSnapshotComponent.getComponentType());
    if (snapshot == null || snapshot.getStructureType() != type) {
      return null;
    }

    return result;
  }
}
