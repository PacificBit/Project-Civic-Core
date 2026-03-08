package com.pacificbytestudios.projectciviccore.village.systems.workplace;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.RequestNewTaskComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.WorkplaceComponent;

public class WorkplaceSystem extends EntityTickingSystem<ChunkStore> {

  @Override
  public void tick(
      float deltaTime,
      int index,
      ArchetypeChunk<ChunkStore> archetypeChunk,
      Store<ChunkStore> chunkStore,
      CommandBuffer<ChunkStore> commandBuffer) {
    Ref<ChunkStore> workplaceRef = archetypeChunk.getReferenceTo(index);

    if (workplaceRef == null || !workplaceRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[WorkplaceSystem] Could not get the workplace reference");
      return;
    }

    chunkStore.getExternalData().getWorld().execute(() -> {
      WorkplaceComponent workplaceComponent = chunkStore.getComponent(workplaceRef,
          WorkplaceComponent.getComponentType());

      if (workplaceComponent == null) {
        ProjectCivicCore.LOGGER.atSevere().log("[WorkplaceSystem] Could not get the workplace component");
        return;
      }

      RequestNewTaskComponent request = chunkStore.getComponent(
          workplaceRef,
          RequestNewTaskComponent.getComponentType());

      System.out.println(request);

      if (request.getStrategy().execute(workplaceComponent)) {
        workplaceRef.getStore().removeComponent(workplaceRef,
            RequestNewTaskComponent.getComponentType());
      }
    });

  }

  @Override
  public Query<ChunkStore> getQuery() {
    return Query.and(
        RequestNewTaskComponent.getComponentType(),
        WorkplaceComponent.getComponentType());
  }
}
