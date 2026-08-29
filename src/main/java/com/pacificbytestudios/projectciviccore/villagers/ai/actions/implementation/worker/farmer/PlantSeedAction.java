package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.farmer;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.InventoryHelper;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.VillagerInventoryAccess;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.FarmerScan;
import com.pacificbytestudios.projectciviccore.villagers.enums.Crops;

public class PlantSeedAction extends VillagerActionBase {

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
    WorkplaceAreaSnapshot workplaceSnapshot = this.getAreaSnapshot(ref, store);
    if (workplaceSnapshot == null) {
      return false;
    }

    Vec3i blockPos = workplaceSnapshot.pollNextSnapshotTargetBlockFrom(FarmerScan.PLANTABLE_TERRAIN);
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

    World world = store.getExternalData().getWorld();
    boolean blockSpawned = Utils.setBlock(
        world,
        blockPos,
        BlockType.getAssetMap().getAsset(plantBlockId));

    if (blockSpawned) {
      InventoryHelper.removeItemInHand(ref, store, 1);
      return true;
    }
    return false;
  }

}
