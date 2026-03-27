package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker;

import java.util.ArrayDeque;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemTool;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.BuilderBreakBlockAction;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.WoodCutterTerrainRepair;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.WoodCutterScan;

public class BreakBlockAction extends VillagerActionBase {
  private final String snapshotGroup;
  private final double requiredStoppingRangeSquared;
  private final WoodCutterTerrainRepair terrainRepairer;

  private boolean alreadyClearedTerrain = false;

  public BreakBlockAction(
      BuilderBreakBlockAction builderActionBase,
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
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.canExecute(ref, role, sensorInfo, deltaTime, store)) {
      return false;
    }
    super.execute(ref, role, sensorInfo, deltaTime, store);

    WorkplaceAreaSnapshot snapshot = this.getAreaSnapshot(ref, store);
    NPCEntity npc = this.getNPCEntity(ref, store);
    if (npc == null || snapshot == null) {
      ProjectCivicCore.LOGGER.atWarning().log("[BreakBlockAction] Could not find NPC or snapshot");
      return false;
    }

    ArrayDeque<Vector3i> targets = snapshot.getBlockTypePosition().get(this.snapshotGroup);
    if (targets == null || targets.isEmpty()) {
      ProjectCivicCore.LOGGER.atInfo().log(
          "[BreakBlockAction] No targets available for snapshot group " + this.snapshotGroup);
      return false;
    }

    Vector3i target = targets.peekFirst();
    if (target == null) {
      ProjectCivicCore.LOGGER.atInfo().log(
          "[BreakBlockAction] Snapshot group " + this.snapshotGroup + " returned a null target");
      return false;
    }

    if (this.requiredStoppingRangeSquared >= 0.0) {
      TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
      if (transform == null) {
        ProjectCivicCore.LOGGER.atWarning().log("[BreakBlockAction] Could not find transform component");
        return false;
      }

      double distanceFromTarget = transform.getPosition().distanceSquaredTo(Utils.centerOf(target).toVector3d());
      if (distanceFromTarget > this.requiredStoppingRangeSquared) {
        return false;
      }
    }

    ItemStack itemStackInHand = npc.getInventory().getItemInHand();
    if (itemStackInHand == null || itemStackInHand.getItem() == null) {
      ProjectCivicCore.LOGGER.atInfo().log("[BreakBlockAction] NPC has no valid item in hand");
      return false;
    }

    Item itemInHand = itemStackInHand.getItem();
    ItemTool tool = itemInHand.getTool();
    if (tool == null) {
      ProjectCivicCore.LOGGER.atInfo().log("[BreakBlockAction] Item in hand is not a tool");
      return false;
    }

    World world = store.getExternalData().getWorld();
    WorldChunk chunk = Utils.getWorldChunk(world, target);
    if (chunk == null) {
      ProjectCivicCore.LOGGER.atWarning().log("[BreakBlockAction] Could not load chunk for " + target);
      return false;
    }

    BlockType blockType = Utils.getBlockTypeChunkAware(world, chunk, target);
    if (blockType == null) {
      ProjectCivicCore.LOGGER.atInfo().log("[BreakBlockAction] No block type found at " + target);
      return false;
    }

    if (BlockHarvestUtils.getSpecPowerDamageBlock(itemInHand, blockType, tool) == null) {
      ProjectCivicCore.LOGGER.atInfo().log("[BreakBlockAction] The tool in hand cannot break the target block");
      targets.pollFirst();
      if (targets.isEmpty()) {
        snapshot.getBlockTypePosition().get(WoodCutterScan.CUTTABLE_TREE).pollFirst();
      }
      return false;
    }

    if (!this.alreadyClearedTerrain) {
      this.alreadyClearedTerrain = true;
      this.terrainRepairer.repairTerrainBelowSapling(world,
          snapshot.getBlockTypePosition().get(WoodCutterScan.CUTTABLE_TREE).peekFirst());
    }

    ChunkStore chunkStoreObj = world.getChunkStore();
    long chunkIndex = ChunkUtil.indexChunkFromBlock(target.x, target.z);
    Ref<ChunkStore> chunkRef = chunkStoreObj.getChunkReference(chunkIndex);

    boolean didBreak = BlockHarvestUtils.performBlockDamage(npc, ref, target, itemStackInHand, tool, null,
        false, 1.0f, 0, chunkRef, store, chunkStoreObj.getStore());

    if (didBreak) {
      targets.pollFirst();
      if (targets.isEmpty()) {
        snapshot.getBlockTypePosition().get(WoodCutterScan.CUTTABLE_TREE).pollFirst();
        this.alreadyClearedTerrain = false;
      }
    }

    return didBreak;
  }
}
