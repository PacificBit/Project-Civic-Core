package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.woodcutter;

import java.util.ArrayDeque;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.InventoryHelper;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.woodcutter.BuilderPlantTreeAction;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
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
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.canExecute(ref, role, sensorInfo, deltaTime, store)) {
      return false;
    }
    super.execute(ref, role, sensorInfo, deltaTime, store);

    VillagerComponent villager = this.getVillagerComponent(ref, store);
    NPCEntity npcEntity = this.getNPCEntity(ref, store);
    if (villager == null || npcEntity == null) {
      return false;
    }

    ArrayDeque<Vector3i> blockPositions = villager
        .getWorkerData()
        .getWorkareaSnapshot()
        .getBlockTypePosition().get(WoodCutterScan.PLANTABLE_TERRAIN);

    if (blockPositions == null || blockPositions.isEmpty()) {
      throw new IllegalStateException("[PlantTree] No Plantable soil block available");
    }

    Vector3i blockPos = blockPositions.peekFirst();
    if (blockPos == null) {
      ProjectCivicCore.LOGGER.atWarning().log("[PlantTree] Snapshot returned a null plant target");
      return false;
    }

    ItemStack sapling = npcEntity.getInventory().getActiveHotbarItem();

    if (sapling == null || sapling.getItemId() == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[PlantTree] Could not find a sapling at hand, this should be called when a sapling is held");
      return false;
    }

    World world = store.getExternalData().getWorld();
    this.terrainRepairer.repairTerrainBelowSapling(world, blockPos);

    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));
    if (chunk == null) {
      ProjectCivicCore.LOGGER.atWarning().log("[PlantTree] Could not load chunk for " + blockPos);
      return false;
    }

    boolean blockSpawned = chunk.setBlock(blockPos.x, blockPos.y, blockPos.z,
        BlockType.getAssetMap().getAsset(sapling.getItemId()));

    if (blockSpawned) {
      blockPositions.pollFirst();
      InventoryHelper.removeItemInHand(ref, store, 1);
      return true;
    }

    ProjectCivicCore.LOGGER.atFine().log("[PlantTree] Failed to place sapling at " + blockPos);
    return false;
  }
}
