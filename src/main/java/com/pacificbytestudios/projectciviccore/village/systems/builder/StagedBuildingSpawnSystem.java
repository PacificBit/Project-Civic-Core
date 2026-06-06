package com.pacificbytestudios.projectciviccore.village.systems.builder;

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
import com.pacificbytestudios.projectciviccore.codecs.BlockContext;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.builder.BuildingSnapshotComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.RegisterStructureTopologyTask;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class StagedBuildingSpawnSystem extends RefSystem<ChunkStore> {
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

    WorldChunk chunk = Utils.getWorldChunk(blockContext);
    if (chunk == null) {
      Logger.logWarning("Missing world chunk");
      return;
    }
    World world = chunk.getWorld();
    Vec3i blockPos = blockContext.getPosition();

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
      BuildingSnapshotComponent snapshotComponent = store.getComponent(ref,
          BuildingSnapshotComponent.getComponentType());

      Logger.logInfo("Registering topology of new building with id: "
              + snapshotComponent.getStructureId());

      VillageTaskQueueComponent taskComponent = new VillageTaskQueueComponent();
      taskComponent.registerTask(new RegisterStructureTopologyTask(
          snapshotComponent.getStructureId(),
          snapshotComponent.getStructureType(),
          snapshotComponent.getPrefabAnchor(),
          snapshotComponent.getRotation()));

      Utils.safeVillageTaskEnqueue(heartRef, taskComponent);
    });
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
    return BuildingSnapshotComponent.getComponentType();
  }
}
