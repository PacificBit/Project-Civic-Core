package com.pacificbytestudios.projectciviccore.village.core.spawner;

import java.util.UUID;

import org.joml.Vector3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.village.components.builder.BuildingSnapshotComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.enums.ConstructionMode;

public class MarketSpawner {

  public static boolean beginNewConstruction(
      UUID structureId,
      WorldChunk chunk,
      Vector3i prefabAnchor,
      Rotation rotation,
      VillageHeartComponent heart,
      int targetLevel,
      ConstructionMode constructionMode) {
    Rotation structureRotation = StructurePlacementUtils.normalize(rotation);

    if (constructionMode == ConstructionMode.COMPLETED) {
      return handleNewCompletedConstruction(structureId, chunk, prefabAnchor,
          structureRotation, heart, targetLevel);
    }
    return handleNewStagedConstruction(structureId, chunk, prefabAnchor,
        structureRotation, heart, targetLevel);
  }

  private static boolean handleNewCompletedConstruction(
      UUID structureId,
      WorldChunk chunk,
      Vector3i prefabAnchor,
      Rotation structureRotation,
      VillageHeartComponent heart,
      int targetLevel) {
    return true;
  }

  private static boolean handleNewStagedConstruction(
      UUID structureId,
      WorldChunk chunk,
      Vector3i prefabAnchor,
      Rotation structureRotation,
      VillageHeartComponent heart,
      int targetLevel) {
    return true;
  }

  public static boolean beginUpgradeConstruction(
      WorldChunk chunk,
      Vector3i existingCoreOrSitePosition,
      int targetLevel,
      ConstructionMode mode) {
    StructureConfig.Tavern.getLevelLayout(2).workplace().rotation();
    return true;
  }

  public static boolean advanceConstruction(
      WorldChunk chunk,
      Vector3i prefabAnchor,
      Rotation rotation) {
    return false;
  }

  private static boolean removePreviousStagePrefab(
      WorldChunk chunk,
      Store<ChunkStore> store,
      Ref<ChunkStore> constructionSignRef,
      Vector3i constructionSignPosition,
      BuildingSnapshotComponent snapshot) {
    return false;
  }

  private static boolean spawnMiddleStageBuilding(
      WorldChunk chunk,
      Store<ChunkStore> store,
      Ref<ChunkStore> constructionSignRef,
      Vector3i constructionSignPosition,
      BuildingSnapshotComponent snapshot) {
    return false;
  }

  private static boolean handleLastStageBuilding(
      UUID structureId,
      World world,
      WorldChunk chunk,
      Vector3i prefabAnchor,
      Rotation rotation,
      VillageHeartComponent heart,
      int targetLevel,
      TrackedPrefabPlacementComponent trackedPlacement) {
    return false;
  }


  public static boolean despawnIncompleteBuilding(
      WorldChunk chunk,
      Vector3i prefabAnchor,
      int level,
      Rotation rotation) {
        return false;
  }

  public static boolean despawn(WorldChunk chunk, Vector3i position) {
    return false;
  }

  private static void removeConstructionSignIfPresent(
      WorldChunk chunk,
      Vector3i prefabAnchor,
      int level,
      Rotation rotation) {
  }
}
