package com.pacificbytestudios.projectciviccore.village.systems.workplace;

import java.util.UUID;

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
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.Utils.BlockContext;
import com.pacificbytestudios.projectciviccore.village.codec.VillageHeartRef;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.WorkplaceSpawnComponent;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

public class WorkplaceSpawnSystem extends RefSystem<ChunkStore> {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  @Override
  public void onEntityAdded(
      Ref<ChunkStore> ref,
      AddReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    BlockContext blockContext = Utils.getWorldBlockContext(ref, commandBuffer);

    assert blockContext != null;

    WorldChunk chunk = blockContext.chunk();
    World world = chunk.getWorld();
    Vector3i blockPos = blockContext.position();

    VillageHeartRef villageHeartRef = villageStore.getVillageHeartIn(new Vector2i(blockPos.x, blockPos.z));
    Ref<ChunkStore> heartRef = villageHeartRef.getRef();

    world.execute(() -> {
      VillageHeartComponent heartComponent = store.getComponent(heartRef, VillageHeartComponent.getComponentType());
      WorkplaceComponent workplaceComponent = store.getComponent(ref, WorkplaceComponent.getComponentType());

      workplaceComponent.setUuid(UUID.randomUUID());
      // heartComponent.registerWorkplace(workplaceComponent);

      store.removeComponent(ref, WorkplaceSpawnComponent.getComponentType());
    });
  }

  @Override
  public void onEntityRemove(
      Ref<ChunkStore> ref,
      RemoveReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    // BlockContext blockContext = Utils.getWorldBlockContext(ref, commandBuffer);
    //
    // assert blockContext != null;
    //
    // WorldChunk chunk = blockContext.chunk();
    // World world = chunk.getWorld();
    // Vector3i blockPos = blockContext.position();
    //
    // VillageHeartRef villageHeartRef = villageStore.getVillageHeartIn(new
    // Vector2i(blockPos.x, blockPos.z));
    // Ref<ChunkStore> heartRef = villageHeartRef.getRef();
    //
    // world.execute(() -> {
    // VillageHeartComponent heartComponent = store.getComponent(heartRef,
    // VillageHeartComponent.getComponentType());
    // WorkplaceComponent workplaceComponent = store.getComponent(ref,
    // WorkplaceComponent.getComponentType());
    // });
  }

  @Override
  public Query<ChunkStore> getQuery() {
    return Query.and(WorkplaceComponent.getComponentType(), WorkplaceSpawnComponent.getComponentType());
  }
}
