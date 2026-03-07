package com.pacificbytestudios.projectciviccore.utils;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

@FunctionalInterface
public interface SpawnFunction {
  boolean apply(WorldChunk chunk, Vector3i position);
}
