package com.pacificbytestudios.projectciviccore.utils;

import java.util.UUID;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.enums.ConstructionMode;

public class SpawnerFunctions {

  @FunctionalInterface
  public interface BeginNewConstruction {
    boolean apply(
        UUID structureId,
        WorldChunk chunk,
        Vector3i prefabAnchor,
        Rotation rotation,
        VillageHeartComponent village,
        int targetLevel,
        ConstructionMode constructionMode);
  }

  @FunctionalInterface
  public interface BeginUpgradeConstruction {
    boolean apply(
        WorldChunk chunk,
        Vector3i existingCoreOrSitePosition,
        int targetLevel,
        ConstructionMode mode);
  }

  @FunctionalInterface
  public interface AdvanceConstructionStage {
    boolean apply(
        WorldChunk chunk,
        Vector3i prefabAnchor,
        Rotation rotation);
  }
}
