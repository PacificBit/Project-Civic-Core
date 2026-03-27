package com.pacificbytestudios.projectciviccore.village.systems.workplace;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.Utils.BlockContext;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceSpawnComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.RegisterWorkplaceTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.TryJobAssignmentTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.UnregisterWorkplaceTask;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

public class WorkplaceSpawnSystem extends RefSystem<ChunkStore> {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  @Override
  public void onEntityAdded(
      Ref<ChunkStore> ref,
      AddReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    if (!reason.equals(AddReason.SPAWN)) {
      return;
    }

    BlockContext blockContext = Utils.getWorldBlockContext(ref, commandBuffer);
    if (blockContext == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[WorkplaceSpawnSystem - onEntityAdded] Missing block context");
      return;
    }

    WorldChunk chunk = blockContext.chunk();
    World world = chunk.getWorld();
    Vector3i blockPos = blockContext.position();

    EntityRef villageHeartRef = villageStore.getVillageHeartIn(new Vector2i(blockPos.x, blockPos.z));
    if (villageHeartRef == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[WorkplaceSpawnSystem - onEntityAdded] No village found");
      return;
    }
    Ref<ChunkStore> heartRef = villageHeartRef.getRef();
    if (heartRef == null || !heartRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[WorkplaceSpawnSystem - onEntityAdded] Village heart ref is invalid");
      return;
    }

    world.execute(() -> {
      WorkplaceComponent workplaceComponent = store.getComponent(ref,
          WorkplaceComponent.getComponentType());
      if (workplaceComponent == null) {
        ProjectCivicCore.LOGGER.atSevere().log("[WorkplaceSpawnSystem - onEntityAdded] Workplace component not found");
        return;
      }

      workplaceComponent.setBenchPos(blockPos);
      workplaceComponent.setVillageRef(villageHeartRef);

      if (workplaceComponent.getUuid() == null) {
        ProjectCivicCore.LOGGER.atWarning()
            .log("[WorkplaceSpawnSystem - onEntityAdded] Workplace UUID is still null after spawn initialization at "
                + blockPos);
        return;
      }

      VillageTaskQueueComponent taskComponent = new VillageTaskQueueComponent();
      taskComponent.registerTask(new RegisterWorkplaceTask(workplaceComponent));
      taskComponent.registerTask(new TryJobAssignmentTask(workplaceComponent.getUuid()));

      Utils.safeVillageTaskEnqueue(heartRef, taskComponent);
    });
  }

  @Override
  public void onEntityRemove(
      Ref<ChunkStore> ref,
      RemoveReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    if (!reason.equals(RemoveReason.REMOVE)) {
      return;
    }

    BlockContext blockContext = Utils.getWorldBlockContext(ref, commandBuffer);

    assert blockContext != null;

    WorldChunk chunk = blockContext.chunk();
    World world = chunk.getWorld();
    Vector3i blockPos = blockContext.position();

    EntityRef villageHeartRef = villageStore.getVillageHeartIn(new Vector2i(blockPos.x, blockPos.z));
    if (villageHeartRef == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[WorkplaceSpawnSystem - onEntityRemove] No village found");
      return;
    }
    Ref<ChunkStore> heartRef = villageHeartRef.getRef();

    WorkplaceComponent workplaceComponent = store.getComponent(ref,
        WorkplaceComponent.getComponentType());

    VillageTaskQueueComponent existingTaskComponent = store.getComponent(heartRef,
        VillageTaskQueueComponent.getComponentType());

    VillageTaskQueueComponent taskComponent = existingTaskComponent != null ? existingTaskComponent
        : new VillageTaskQueueComponent();

    taskComponent.registerTask(new UnregisterWorkplaceTask(workplaceComponent.getUuid()));

    world.execute(() -> {
      store.putComponent(heartRef, VillageTaskQueueComponent.getComponentType(),
          taskComponent);
    });
  }

  @Override
  public Query<ChunkStore> getQuery() {
    return Query.and(WorkplaceComponent.getComponentType(),
        WorkplaceSpawnComponent.getComponentType());
  }
}
