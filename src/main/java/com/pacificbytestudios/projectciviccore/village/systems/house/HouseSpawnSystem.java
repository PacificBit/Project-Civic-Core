package com.pacificbytestudios.projectciviccore.village.systems.house;

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
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseSpawnComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.RegisterHouseTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.TryHouseAssignmentTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.UnregisterHouseTask;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

public class HouseSpawnSystem extends RefSystem<ChunkStore> {
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
      ProjectCivicCore.LOGGER.atSevere().log("[HouseSpawnSystem - onEntityAdded] Missing block context");
      return;
    }

    WorldChunk chunk = blockContext.chunk();
    World world = chunk.getWorld();
    Vector3i blockPos = blockContext.position();

    EntityRef villageHeartRef = villageStore.getVillageHeartIn(new Vector2i(blockPos.x, blockPos.z));
    if (villageHeartRef == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[HouseSpawnSystem - onEntityAdded] No village found");
      return;
    }

    Ref<ChunkStore> heartRef = villageHeartRef.getRef();
    if (heartRef == null || !heartRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[HouseSpawnSystem - onEntityAdded] Village heart ref is invalid");
      return;
    }

    world.execute(() -> {
      HouseComponent houseComponent = store.getComponent(ref,
          HouseComponent.getComponentType());
      if (houseComponent == null) {
        ProjectCivicCore.LOGGER.atSevere().log("[HouseSpawnSystem - onEntityAdded] House component not found");
        return;
      }

      if (houseComponent.getUuid() == null) {
        ProjectCivicCore.LOGGER.atSevere()
            .log("[HouseSpawnSystem - onEntityAdded] House UUID is still null after spawn initialization");
        return;
      }

      houseComponent.setVillageRef(villageHeartRef);

      VillageTaskQueueComponent taskComponent = new VillageTaskQueueComponent();
      taskComponent.registerTask(new RegisterHouseTask(houseComponent));
      taskComponent.registerTask(new TryHouseAssignmentTask(houseComponent.getUuid()));

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
      ProjectCivicCore.LOGGER.atSevere().log("[HouseSpawnSystem - onEntityRemove] Missing block context");
      return;
    }

    WorldChunk chunk = blockContext.chunk();
    World world = chunk.getWorld();
    Vector3i blockPos = blockContext.position();

    EntityRef villageHeartRef = villageStore.getVillageHeartIn(new Vector2i(blockPos.x, blockPos.z));
    if (villageHeartRef == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[HouseSpawnSystem - onEntityRemove] No village found");
      return;
    }

    Ref<ChunkStore> heartRef = villageHeartRef.getRef();
    if (heartRef == null || !heartRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[HouseSpawnSystem - onEntityRemove] Village heart ref is invalid");
      return;
    }

    HouseComponent houseComponent = store.getComponent(ref,
        HouseComponent.getComponentType());
    if (houseComponent == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[HouseSpawnSystem - onEntityRemove] House component not found");
      return;
    }

    VillageTaskQueueComponent taskComponent = new VillageTaskQueueComponent();
    taskComponent.registerTask(new UnregisterHouseTask(houseComponent.getUuid()));

    world.execute(() -> {
      if (heartRef == null || !heartRef.isValid()) {
        return;
      }

      Utils.safeVillageTaskEnqueue(heartRef, taskComponent);
    });
  }

  @Override
  public Query<ChunkStore> getQuery() {
    return Query.and(HouseComponent.getComponentType(),
        HouseSpawnComponent.getComponentType());
  }

}
