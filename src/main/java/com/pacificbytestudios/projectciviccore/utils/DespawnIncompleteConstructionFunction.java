package com.pacificbytestudios.projectciviccore.utils;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;

@FunctionalInterface
public interface DespawnIncompleteConstructionFunction {
  boolean apply(
      WorldChunk chunk,
      Vec3i prefabAnchor,
      int level,
      Rotation rotation,
      VillageHeartComponent village);
}
