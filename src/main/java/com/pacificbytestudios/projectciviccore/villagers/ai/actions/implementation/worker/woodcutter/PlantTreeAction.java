package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.woodcutter;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.InventoryHelper;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.woodcutter.BuilderPlantTreeAction;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.VillagerInventoryAccess;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.WoodCutterTerrainRepair;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.WoodCutterScan;

public class PlantTreeAction extends VillagerActionBase {
  private final WoodCutterTerrainRepair terrainRepairer;

  public PlantTreeAction(
      BuilderPlantTreeAction builderActionBase,
      BuilderSupport support) {
    super(builderActionBase);
    this.terrainRepairer = new WoodCutterTerrainRepair(builderActionBase.getTerrainRepairBlockIds(support));
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      ExecutionSupport executionSupport,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.canExecute(ref, executionSupport, sensorInfo, deltaTime, store)) {
      return false;
    }
    super.execute(ref, executionSupport, sensorInfo, deltaTime, store);

    WorkplaceAreaSnapshot snapshot = this.getAreaSnapshot(ref, store);
    if (snapshot == null) {
      return false;
    }

    Vec3i blockPos = snapshot
        .peekNextSnapshotTargetBlockFrom(WoodCutterScan.PLANTABLE_TERRAIN);

    ItemStack sapling = VillagerInventoryAccess.activeHotbarItem(store, ref);

    if (sapling == null || sapling.getItemId() == null) {
      Logger.logWarning("Could not find a sapling at hand, this should be called when a sapling is held");
      return false;
    }

    World world = store.getExternalData().getWorld();
    this.terrainRepairer.repairTerrainBelowSapling(world, blockPos);

    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));
    if (chunk == null) {
      Logger.logWarning("Could not load chunk for " + blockPos);
      return false;
    }

    boolean blockSpawned = chunk.setBlock(blockPos.x, blockPos.y, blockPos.z,
        BlockType.getAssetMap().getAsset(sapling.getItemId()));

    if (blockSpawned) {
      snapshot.removeFirstSnapshotTargetFrom(WoodCutterScan.PLANTABLE_TERRAIN);
      InventoryHelper.removeItemInHand(ref, store, 1);
      return true;
    }

    Logger.logFine("Failed to place sapling at " + blockPos);
    return false;
  }
}
