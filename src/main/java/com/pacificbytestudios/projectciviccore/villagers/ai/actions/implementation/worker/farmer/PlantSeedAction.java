package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.farmer;

import java.util.ArrayDeque;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.InventoryHelper;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.VillagerInventoryAccess;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.FarmerScan;
import com.pacificbytestudios.projectciviccore.villagers.enums.Crops;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class PlantSeedAction extends ActionBase {

  public PlantSeedAction(BuilderActionBase builderActionBase) {
    super(builderActionBase);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      ExecutionSupport executionSupport,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    VillagerComponent villager = store.getComponent(ref, VillagerComponent.getComponentType());
    if (villager == null) {
      Logger.logWarning("Could not fetch villager component");
      return false;
    }

    ArrayDeque<Vec3i> blockPositions = villager
        .getWorkerData()
        .getWorkareaSnapshot()
        .getBlockTypePosition().get(FarmerScan.PLANTABLE_TERRAIN);

    Vec3i blockPos;

    if (blockPositions != null && !blockPositions.isEmpty()) {
      blockPos = blockPositions.pollFirst();
    } else {
      throw new IllegalStateException("[PlantSeed] No Plantable soil block available");
    }

    ItemStack seeds = VillagerInventoryAccess.activeHotbarItem(store, ref);

    if (seeds == null || seeds.getItemId() == null) {
      Logger.logWarning("Could not find a seed at hand, this should be called when a seed is held");
      return false;
    }

    String plantBlockId = Crops.getBlockFromSeed(seeds.getItemId());

    if (plantBlockId == null) {
      Logger.logWarning("Invalid seed with id: " + seeds.getItemId());
      return false;
    }

    WorldChunk chunk = store.getExternalData().getWorld()
        .getChunk(ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));

    boolean blockSpawned = chunk.setBlock(blockPos.x, blockPos.y, blockPos.z,
        BlockType.getAssetMap().getAsset(plantBlockId));

    if (blockSpawned) {
      InventoryHelper.removeItemInHand(ref, store, 1);
      return true;
    }
    return false;
  }

}
