package com.pacificbytestudios.projectciviccore.village.systems.heart;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.Utils.BlockContext;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageSpawnComponent;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

public class VillageHeartLoadUnloadSystem extends RefSystem<ChunkStore> {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  @Override
  public void onEntityAdded(
      Ref<ChunkStore> ref,
      AddReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    if (reason.equals(AddReason.SPAWN)) {
      ProjectCivicCore.LOGGER.atInfo().log("[onEntityAdded] - Skippin: " + reason);
      return;
    }
    ProjectCivicCore.LOGGER.atInfo().log("[onEntityAdded] - Loading reason: " + reason);

    BlockContext context = Utils.getWorldBlockContext(ref, commandBuffer);

    if (context == null) {
      ProjectCivicCore.LOGGER.atInfo().log("[onEntityAdded] - No context found");
      return;
    }

    WorldChunk chunk = context.chunk();

    chunk.getWorld().execute(() -> {
      VillageHeartComponent heart = store.getComponent(ref, VillageHeartComponent.getComponentType());

      if (heart == null) {
        ProjectCivicCore.LOGGER.atInfo().log("[onEntityAdded] - Could not find the heart component");
        return;
      }

      if (heart.getClaimedArea() == null) {
        Utils.claimVillageArea(heart, context.position());
      }

      villageStore.registerVillage(
          chunk.getWorld().getName(),
          context.position(),
          heart.getClaimedArea());
    });
  }

  @Override
  public void onEntityRemove(
      Ref<ChunkStore> villageHeartRef,
      RemoveReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    ProjectCivicCore.LOGGER.atInfo().log("[onEntityRemove] - Unloading reason: " + reason);
  }

  @Override
  public Query<ChunkStore> getQuery() {
    return Query.and(
        VillageHeartComponent.getComponentType(),
        Query.not(VillageSpawnComponent.getComponentType()));
  }
}
