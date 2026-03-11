package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.function.BiConsumer;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;

public interface IVillageTask {

  public void execute(WorldChunk chunk, Ref<ChunkStore> heartRef, Vector3i heartPos, VillageHeartComponent heart);

  public void setOnError(BiConsumer<String, Object> callback);

}
