package com.pacificbytestudios.projectciviccore.villageheart.systems;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.villageheart.components.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.villageheart.components.VillageSpawnComponent;
import com.pacificbytestudios.projectciviccore.villageheart.registry.VillageHeartRegistry;
import com.pacificbytestudios.projectciviccore.villageheart.registry.VillageHeartRegistry.ChunkLocation;

public class VillageHeartLoadUnloadSystem extends RefSystem<ChunkStore> {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  @Override
  public void onEntityAdded(
      Ref<ChunkStore> villageHeartRef,
      AddReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    ProjectCivicCore.LOGGER.atInfo().log("[VillageHeartLoadUnloadSystem] - Loading reason: " + reason);
    World world = store.getExternalData().getWorld();
    Vector3i blockPos = Utils.getWorldBlockPosition(villageHeartRef, commandBuffer);

    assert blockPos != null;

    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));
    villageStore.registerVillage(new ChunkLocation(chunk.getX(), chunk.getZ()), villageHeartRef);
  }

  @Override
  public void onEntityRemove(
      Ref<ChunkStore> villageHeartRef,
      RemoveReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    ProjectCivicCore.LOGGER.atInfo().log("[VillageHeartLoadUnloadSystem] - Unloading reason: " + reason);
  }

  @Override
  public Query<ChunkStore> getQuery() {
    return Query.and(
        VillageHeartComponent.getComponentType(),
        Query.not(VillageSpawnComponent.getComponentType()));
  }
}
