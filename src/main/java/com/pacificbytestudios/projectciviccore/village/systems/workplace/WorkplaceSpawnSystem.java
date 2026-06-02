package com.pacificbytestudios.projectciviccore.village.systems.workplace;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D.Vec2i;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
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
import com.pacificbytestudios.projectciviccore.utils.Logger;

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
      Logger.logWarning("Missing block context");
      return;
    }

    WorldChunk chunk = blockContext.chunk();
    World world = chunk.getWorld();
    Vec3i blockPos = blockContext.position();

    EntityRef villageHeartRef = villageStore.getVillageHeartIn(new Vec2i(blockPos.x, blockPos.z));
    if (villageHeartRef == null) {
      Logger.logWarning("No village found");
      return;
    }
    Ref<ChunkStore> heartRef = villageHeartRef.getRef();
    if (heartRef == null || !heartRef.isValid()) {
      Logger.logWarning("Village heart ref is invalid");
      return;
    }

    world.execute(() -> {
      WorkplaceComponent workplaceComponent = store.getComponent(ref,
          WorkplaceComponent.getComponentType());
      if (workplaceComponent == null) {
        Logger.logWarning("Workplace component not found");
        return;
      }

      workplaceComponent.setBenchPos(blockPos);
      workplaceComponent.setVillageRef(villageHeartRef);

      if (workplaceComponent.getUuid() == null) {
        Logger.logWarning("Workplace UUID is still null after spawn initialization at "
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
    if (blockContext == null) {
      Logger.logWarning("Missing block context");
      return;
    }

    WorldChunk chunk = blockContext.chunk();
    World world = chunk.getWorld();
    Vec3i blockPos = blockContext.position();

    EntityRef villageHeartRef = villageStore.getVillageHeartIn(new Vec2i(blockPos.x, blockPos.z));
    if (villageHeartRef == null) {
      Logger.logWarning("No village found");
      return;
    }
    Ref<ChunkStore> heartRef = villageHeartRef.getRef();
    if (heartRef == null || !heartRef.isValid()) {
      Logger.logWarning("Village heart ref is invalid");
      return;
    }

    WorkplaceComponent workplaceComponent = store.getComponent(ref,
        WorkplaceComponent.getComponentType());
    if (workplaceComponent == null) {
      Logger.logWarning("Workplace component not found");
      return;
    }

    VillageTaskQueueComponent taskComponent = new VillageTaskQueueComponent();
    taskComponent.registerTask(new UnregisterWorkplaceTask(workplaceComponent.getUuid()));

    world.execute(() -> {
      if (heartRef == null || !heartRef.isValid()) {
        return;
      }

      Utils.safeVillageTaskEnqueue(heartRef, taskComponent);
    });
  }

  @Override
  public Query<ChunkStore> getQuery() {
    return Query.and(WorkplaceComponent.getComponentType(),
        WorkplaceSpawnComponent.getComponentType());
  }
}
