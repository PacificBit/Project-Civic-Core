package com.pacificbytestudios.projectciviccore.utils;

import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

@FunctionalInterface
public interface DespawnFunction {
  boolean apply(WorldChunk chunk, Vec3i position);
}
