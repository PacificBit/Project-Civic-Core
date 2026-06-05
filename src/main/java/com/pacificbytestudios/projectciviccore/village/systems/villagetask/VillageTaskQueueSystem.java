package com.pacificbytestudios.projectciviccore.village.systems.villagetask;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.Utils.BlockContext;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.AVillageTask;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class VillageTaskQueueSystem extends EntityTickingSystem<ChunkStore> {
  private static final long MAX_TIME_NS = 1_000_000;

  @Override
  public void tick(
      float deltaTime,
      int index,
      ArchetypeChunk<ChunkStore> archetypeChunk,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    Ref<ChunkStore> villageHeartRef = archetypeChunk.getReferenceTo(index);

    if (villageHeartRef == null || !villageHeartRef.isValid()) {
      Logger.logInfo("No valid ref found");
      return;
    }

    BlockContext context = Utils.getWorldBlockContext(villageHeartRef,
        commandBuffer);

    context.chunk().getWorld().execute(() -> {
      VillageHeartComponent villageHeart = store.getComponent(villageHeartRef,
          VillageHeartComponent.getComponentType());
      VillageTaskQueueComponent tasksComponent = villageHeart.getComponent(
          VillageTaskQueueComponent.getComponentType());

      AVillageTask task = tasksComponent.pollTask();

      long start = System.nanoTime();

      while (task != null) {
        task.execute(
            context.chunk(),
            villageHeartRef,
            context.position(),
            villageHeart);

        if (System.nanoTime() - start >= MAX_TIME_NS) {
          break;
        }

        task = tasksComponent.pollTask();
      }

      if (!tasksComponent.hasAny()) {
        store.removeComponent(villageHeartRef,
            VillageTaskQueueComponent.getComponentType());
      }
    });
  }

  @Override
  public Query<ChunkStore> getQuery() {
    return Query.and(VillageHeartComponent.getComponentType(),
        VillageTaskQueueComponent.getComponentType());
  }
}
