package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.farmer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.builtin.adventure.farming.states.TilledSoilBlock;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.FarmerScan;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class WaterSoilAction extends ActionBase {
  private static final long WATERED_DURATION = 86400;

  public WaterSoilAction(BuilderActionBase builderActionBase) {
    super(builderActionBase);
  }

  public boolean execute(
      Ref<EntityStore> ref,
      ExecutionSupport executionSupport,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    super.execute(ref, executionSupport, sensorInfo, deltaTime, store);

    VillagerComponent villager = store.getComponent(ref, VillagerComponent.getComponentType());
    if (villager == null) {
      Logger.logWarning("Could not fetch villager component");
      return false;
    }

    ArrayDeque<Vec3i> blockPositions = villager
        .getWorkerData()
        .getWorkareaSnapshot()
        .getBlockTypePosition().get(FarmerScan.WATERABLE_TERRAIN);

    Vec3i blockPos;

    if (blockPositions != null && !blockPositions.isEmpty()) {
      blockPos = blockPositions.pollFirst();
    } else {
      throw new IllegalStateException("No waterable soil block available");
    }

    World world = store.getExternalData().getWorld();
    return waterTilledSoil(world, blockPos);
  }

  public static boolean waterTilledSoil(World world, Vec3i soilPos) {
    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(soilPos.x, soilPos.z));

    Ref<ChunkStore> blockEntityRef = chunk.getBlockComponentEntity(soilPos.x, soilPos.y, soilPos.z);
    if (blockEntityRef == null) {
      Logger.logWarning("Could not fetch the soil block entityRef");
      return false;
    }

    Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
    Store<EntityStore> entityStore = world.getEntityStore().getStore();

    TilledSoilBlock tilledSoil = chunkStore.getComponent(blockEntityRef, TilledSoilBlock.getComponentType());
    if (tilledSoil == null) {
      Logger.logWarning("Tilled soil component was removed from the block");
      return false;
    }

    WorldTimeResource worldTimeResource = entityStore.getResource(WorldTimeResource.getResourceType());
    Instant wateredUntil = worldTimeResource.getGameTime().plus(WATERED_DURATION, ChronoUnit.SECONDS);
    tilledSoil.setWateredUntil(wateredUntil);

    chunk.setTicking(soilPos.x, soilPos.y, soilPos.z, true);
    chunk.setTicking(soilPos.x, soilPos.y + 1, soilPos.z, true);

    return true;
  }
}
