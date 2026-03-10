package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.Farmer;

import java.util.ArrayDeque;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.InventoryHelper;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.FarmerScan;
import com.pacificbytestudios.projectciviccore.villagers.enums.Crops;

public class PlantSeedAction extends ActionBase {

  public PlantSeedAction(BuilderActionBase builderActionBase) {
    super(builderActionBase);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    VillagerComponent villager = store.getComponent(ref, VillagerComponent.getComponentType());

    if (villager == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[PlantSeed] Could not fetch villager component");
      return false;
    }

    ArrayDeque<Vector3i> blockPositions = villager
        .getWorkerData()
        .getWorkareaSnapshot()
        .getBlockTypePosition().get(FarmerScan.PLANTABLE_TERRAIN);

    Vector3i blockPos;

    if (blockPositions != null && !blockPositions.isEmpty()) {
      blockPos = blockPositions.pollFirst();
    } else {
      throw new IllegalStateException("[PlantSeed] No Plantable soil block available");
    }

    NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
    ItemStack seeds = npcEntity.getInventory().getActiveHotbarItem();

    if (seeds == null || seeds.getItemId() == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[PlantSeed] Could not find a seed at hand, this should be called when a seed is held");
      return false;
    }

    String plantBlockId = Crops.getBlockFromSeed(seeds.getItemId());

    if (plantBlockId == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[PlantSeed] Invalid seed with id: " + seeds.getItemId());
      return false;
    }

    WorldChunk chunk = store.getExternalData().getWorld()
        .getChunk(ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));

    boolean blockSpawned = chunk.setBlock(blockPos.x, blockPos.y, blockPos.z,
        BlockType.getAssetMap().getAsset(plantBlockId));

    if (blockSpawned) {
      InventoryHelper.removeItemInHand(npcEntity.getInventory(), 1);
      return true;
    }
    return false;
  }

}
