package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.miner;

import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import org.joml.Vector3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemTool;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.InventoryHelper;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.registry.registry.MinerDropRegistry;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineshaftGallery;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.miner.BuilderBreakBlockInSnapshotAction;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerToolAnimationHelper;

public class BreakBlockInSnapshotAction extends VillagerActionBase {
  private final String primarySnapshotGroup;
  private final String secondarySnapshotGroup;
  private final double requiredStoppingRangeSquared;
  private final DropType dropType;
  private final double dropGenerationChance;

  public BreakBlockInSnapshotAction(
      BuilderBreakBlockInSnapshotAction builderActionBase,
      BuilderSupport support) {
    super(builderActionBase);
    this.primarySnapshotGroup = builderActionBase.getPrimarySnapshotGroup(support);
    this.secondarySnapshotGroup = builderActionBase.getSecondarySnapshotGroup(support);
    this.dropType = builderActionBase.getDropType(support);
    this.dropGenerationChance = builderActionBase.getDropGenerationChance(support);

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
    MineComponent mineComponent = this.getComponentFromWorkplace(store, ref, MineComponent.getComponentType());
    UUID villagerId = this.getVillagerId(ref, store);
    if (snapshot == null || npc == null || mineComponent == null || villagerId == null) {
      return false;
    }

    ArrayDeque<Vector3i> targets = snapshot.getBlockTypePosition().get(this.primarySnapshotGroup);
    if (targets == null) {
      return false;
    }

    if (targets.isEmpty()) {
      this.finishCurrentDigPosition(snapshot, mineComponent, villagerId);
      return false;
    }

    Vector3i target = targets.peekFirst();
    if (target == null) {
      targets.pollFirst();
      if (targets.isEmpty()) {
        this.finishCurrentDigPosition(snapshot, mineComponent, villagerId);
      }
      return false;
    }

    if (this.requiredStoppingRangeSquared >= 0.0) {
      TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
      if (transform == null) {
        ProjectCivicCore.LOGGER.atWarning().log("[BreakBlockInSnapshotAction] Could not find transform component");
        return false;
      }

      Vector3i rangeAnchor = this.resolveRangeAnchor(snapshot, target);
      double distanceFromAnchor = transform.getPosition().distanceSquared(
          VectorUtil.toVector3d(Utils.centerOf(rangeAnchor)));
      if (distanceFromAnchor > this.requiredStoppingRangeSquared) {
        return false;
      }
    }

    World world = this.getWorld(store);
    WorldChunk chunk = Utils.getWorldChunk(world, target);
    if (chunk == null) {
      ProjectCivicCore.LOGGER.atWarning().log("[BreakBlockInSnapshotAction] Could not load chunk for " + target);
      return false;
    }

    BlockType blockType = Utils.getBlockTypeChunkAware(world, chunk, target);
    if (blockType == null) {
      ProjectCivicCore.LOGGER.atInfo().log("[BreakBlockInSnapshotAction] No block type found at " + target);
      return false;
    }

    ItemStack itemStackInHand = npc.getInventory().getItemInHand();
    if (itemStackInHand == null || itemStackInHand.getItem() == null) {
      ProjectCivicCore.LOGGER.atInfo().log("[BreakBlockInSnapshotAction] NPC has no valid item in hand");
      return false;
    }

    Item itemInHand = itemStackInHand.getItem();
    ItemTool tool = itemInHand.getTool();
    if (tool == null) {
      ProjectCivicCore.LOGGER.atInfo().log("[BreakBlockInSnapshotAction] Item in hand is not a tool");
      return false;
    }

    ChunkStore chunkStoreObj = world.getChunkStore();
    long chunkIndex = ChunkUtil.indexChunkFromBlock(target.x, target.z);
    Ref<ChunkStore> chunkRef = chunkStoreObj.getChunkReference(chunkIndex);

    VillagerToolAnimationHelper.playHeldToolAnimation(ref, npc, store, "Mine");
    boolean didBreak = BlockHarvestUtils.performBlockDamage(ref, target, itemStackInHand, tool, null,
        false, 1.0f, 0, chunkRef, store, chunkStoreObj.getStore());

    if (didBreak) {
      this.consumeToolDurability(ref, store);
      this.generateDropIfNeeded(ref, store);
      targets.pollFirst();
      if (targets.isEmpty()) {
        this.finishCurrentDigPosition(snapshot, mineComponent, villagerId);
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

  private Vector3i resolveRangeAnchor(WorkplaceAreaSnapshot snapshot, Vector3i fallbackTarget) {
    ArrayDeque<Vector3i> digPositions = snapshot.getBlockTypePosition().get(this.secondarySnapshotGroup);
    if (digPositions == null || digPositions.isEmpty()) {
      return fallbackTarget;
    }

    Vector3i digPosition = digPositions.peekFirst();
    return digPosition == null ? fallbackTarget : digPosition;
  }

  private void finishCurrentDigPosition(
      WorkplaceAreaSnapshot snapshot,
      MineComponent mineComponent,
      UUID villagerId) {
    snapshot.getBlockTypePosition().remove(this.primarySnapshotGroup);

    ArrayDeque<Vector3i> digPositions = snapshot.getBlockTypePosition().get(this.secondarySnapshotGroup);
    if (digPositions == null || digPositions.isEmpty()) {
      return;
    }

    digPositions.pollFirst();
    this.persistNextDigPosition(mineComponent, villagerId, digPositions.peekFirst());
    if (digPositions.isEmpty()) {
      snapshot.getBlockTypePosition().remove(this.secondarySnapshotGroup);
    }
  }

  private void persistNextDigPosition(
      MineComponent mineComponent,
      UUID villagerId,
      Vector3i nextDigPosition) {
    if (mineComponent == null || villagerId == null || nextDigPosition == null) {
      return;
    }

    MineshaftGallery gallery = mineComponent.getWorkerAllocatedGallery(villagerId);
    if (gallery == null) {
      return;
    }

    gallery.setLastDiggedPosition(VectorUtil.clone(nextDigPosition));
  }

  private void generateDropIfNeeded(Ref<EntityStore> ref, Store<EntityStore> store) {
    if (!this.rollChance(this.dropGenerationChance)) {
      return;
    }

    VillageHeartComponent villageHeart = this.getVillageHeartComponent(ref, store);
    ItemContainer villagerStorage = this.getVillagerStorage(ref, store);
    if (villageHeart == null || villagerStorage == null) {
      return;
    }

    String dropId = this.getDrop(this.dropType, villageHeart.getLevel());
    if (dropId == null || dropId.isBlank()) {
      ProjectCivicCore.LOGGER.atWarning().log("[BreakBlockInSnapshotAction] No drop id generated for drop type "
          + this.dropType);
      return;
    }

    if (Item.getAssetMap().getAsset(dropId) == null) {
      ProjectCivicCore.LOGGER.atWarning().log("[BreakBlockInSnapshotAction] Invalid generated drop id " + dropId);
      return;
    }

    ItemStack dropStack = new ItemStack(dropId, 1);
    if (!villagerStorage.canAddItemStack(dropStack)) {
      ProjectCivicCore.LOGGER.atInfo().log("[BreakBlockInSnapshotAction] Could not store generated drop " + dropId);
      return;
    }

    villagerStorage.addItemStack(dropStack);
  }

  private boolean rollChance(double chancePercent) {
    double clampedChance = Math.max(0.0, Math.min(100.0, chancePercent));
    if (clampedChance <= 0.0) {
      return false;
    }
    if (clampedChance >= 100.0) {
      return true;
    }

    return ThreadLocalRandom.current().nextDouble(100.0) < clampedChance;
  }

  private String getDrop(DropType dropType, int villageLevel) {
    if (dropType == null) {
      return null;
    }

    switch (dropType) {
      case MINER:
        return MinerDropRegistry.getDropByVillageLevel(villageLevel);
      default:
        return null;
    }
  }

  public enum DropType implements Supplier<String> {
    MINER("Miner");

    private final String id;

    private DropType(String id) {
      this.id = id;
    }

    @Override
    public String get() {
      return this.id;
    }
  }
}
