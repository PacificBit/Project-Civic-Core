package com.pacificbytestudios.projectciviccore.utils;

import org.joml.Vector3i;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

@FunctionalInterface
public interface DespawnIncompleteConstructionFunction {
  boolean apply(
      WorldChunk chunk,
      Vector3i prefabAnchor,
      int level,
      Rotation rotation);
}
