package com.pacificbytestudios.projectciviccore.village.systems.heart;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartMarkerComponent;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;

public class VillageHeartMarkerPlacementSystem extends RefSystem<ChunkStore> {

  @Override
  public void onEntityAdded(
      Ref<ChunkStore> ref,
      AddReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    Utils.BlockContext context = Utils.getWorldBlockContext(ref, commandBuffer);
    if (context == null) {
      context = Utils.getWorldBlockContext(ref, store);
    }
    if (context == null) {
      return;
    }

    WorldChunk chunk = context.chunk();
    Vector3i markerPosition = context.position();
    chunk.getWorld().execute(() -> Utils.setBlockChunkAware(
        chunk,
        markerPosition,
        StructureConfig.TownHeart.HEART_BLOCK));
  }

  @Override
  public void onEntityRemove(
      Ref<ChunkStore> ref,
      RemoveReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
  }

  @Override
  public Query<ChunkStore> getQuery() {
    return Query.and(
        VillageHeartMarkerComponent.getComponentType(),
        BlockModule.BlockStateInfo.getComponentType());
  }
}
