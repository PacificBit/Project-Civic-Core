package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.woodcutter;

import java.util.ArrayDeque;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemTool;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.InventoryHelper;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.woodcutter.BuilderBreakTreeBlockAction;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.VillagerInventoryAccess;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerToolAnimationHelper;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.WoodCutterTerrainRepair;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.WoodCutterScan;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class BreakTreeBlockAction extends VillagerActionBase {
  private final String snapshotGroup;
  private final double requiredStoppingRangeSquared;
  private final WoodCutterTerrainRepair terrainRepairer;

  private boolean alreadyClearedTerrain = false;

  public BreakTreeBlockAction(
      BuilderBreakTreeBlockAction builderActionBase,
      BuilderSupport support) {
    super(builderActionBase);
    this.snapshotGroup = builderActionBase.getSnapshotGroup(support);

    this.terrainRepairer = new WoodCutterTerrainRepair(builderActionBase.getTerrainRepairBlockIds(support));
    double requiredStoppingRange = builderActionBase.getRequiredStoppingRange(support);
    this.requiredStoppingRangeSquared = requiredStoppingRange < 0.0
        ? requiredStoppingRange
        : requiredStoppingRange * requiredStoppingRange;
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
    NPCEntity npc = this.getNPCEntity(ref, store);
    if (npc == null || snapshot == null) {
      Logger.logWarning("Could not find NPC or snapshot");
      return false;
    }

    ArrayDeque<Vec3i> targets = snapshot.getSnapshotTargets().get(this.snapshotGroup);
    if (targets == null || targets.isEmpty()) {
      Logger.logInfo(
          "No targets available for snapshot group " + this.snapshotGroup);
      return false;
    }

    Vec3i target = targets.peekFirst();
    if (target == null) {
      Logger.logInfo(
          "Snapshot group " + this.snapshotGroup + " returned a null target");
      return false;
    }

    if (this.requiredStoppingRangeSquared >= 0.0) {
      TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
      if (transform == null) {
        Logger.logWarning("Could not find transform component");
        return false;
      }

      double distanceFromTarget = transform.getPosition().distanceSquared(
          Vec3d.from(Utils.centerOf(target)));
      if (distanceFromTarget > this.requiredStoppingRangeSquared) {
        return false;
      }
    }

    ItemStack itemStackInHand = VillagerInventoryAccess.heldItem(store, ref);
    if (itemStackInHand == null || itemStackInHand.getItem() == null) {
      Logger.logInfo("NPC has no valid item in hand");
      return false;
    }

    Item itemInHand = itemStackInHand.getItem();
    ItemTool tool = itemInHand.getTool();
    if (tool == null) {
      Logger.logInfo("Item in hand is not a tool");
      return false;
    }

    World world = store.getExternalData().getWorld();
    WorldChunk chunk = Utils.getWorldChunk(world, target);
    if (chunk == null) {
      Logger.logWarning("Could not load chunk for " + target);
      return false;
    }

    BlockType blockType = Utils.getBlockTypeChunkAware(world, chunk, target);
    if (blockType == null) {
      Logger.logInfo("No block type found at " + target);
      return false;
    }

    if (BlockHarvestUtils.getSpecPowerDamageBlock(itemInHand, blockType, tool) == null) {
      Logger.logInfo("The tool in hand cannot break the target block");
      targets.pollFirst();
      if (targets.isEmpty()) {
        snapshot.getSnapshotTargets().get(WoodCutterScan.CUTTABLE_TREE).pollFirst();
      }
      return false;
    }

    if (!this.alreadyClearedTerrain) {
      this.alreadyClearedTerrain = true;
      this.terrainRepairer.repairTerrainBelowSapling(world,
          snapshot.getSnapshotTargets().get(WoodCutterScan.CUTTABLE_TREE).peekFirst());
    }

    ChunkStore chunkStoreObj = world.getChunkStore();
    long chunkIndex = ChunkUtil.indexChunkFromBlock(target.x, target.z);
    Ref<ChunkStore> chunkRef = chunkStoreObj.getChunkReference(chunkIndex);

    VillagerToolAnimationHelper.playHeldToolAnimation(ref, npc, store, "Chop");
    boolean didBreak = BlockHarvestUtils.performBlockDamage(ref, target, itemStackInHand, tool, null, false, 1, 0,
        false, chunkRef, store, chunkStoreObj.getStore());

    if (didBreak) {
      this.consumeToolDurability(ref, store);
      targets.pollFirst();
      if (targets.isEmpty()) {
        snapshot.getSnapshotTargets().get(WoodCutterScan.CUTTABLE_TREE).pollFirst();
        this.alreadyClearedTerrain = false;
      }
    }

    return didBreak;
  }

  private void consumeToolDurability(
      Ref<EntityStore> ref,
      Store<EntityStore> store) {
    InventoryComponent.Hotbar hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
    if (hotbar == null) {
      return;
    }

    ItemStack holdingItem = hotbar.getActiveItem();
    if (holdingItem == null) {
      return;
    }

    hotbar.getInventory().setItemStackForSlot(
        hotbar.getActiveSlot(),
        holdingItem.withDurability(holdingItem.getDurability() - 1));

    ItemStack updatedItem = hotbar.getActiveItem();
    if (updatedItem != null && updatedItem.isBroken()) {
      InventoryHelper.removeItemInHand(ref, store, 1);
    }
  }
}
