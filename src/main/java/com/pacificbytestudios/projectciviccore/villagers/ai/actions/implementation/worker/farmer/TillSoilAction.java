package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.farmer;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.InventoryHelper;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.VillagerInventoryAccess;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerToolAnimationHelper;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.FarmerScan;

public class TillSoilAction extends VillagerActionBase {
  private static final BlockType TILL_BLOCK_TYPE = BlockType.getAssetMap().getAsset(FarmerScan.TILLED_SOIL_ID);
  private static final int TILL_SOUND_EVENT_INDEX = SoundEvent.getAssetMap().getIndex("SFX_Hoe_T1_Till");

  public TillSoilAction(BuilderActionBase builderActionBase) {
    super(builderActionBase);
  }

  public boolean execute(
      Ref<EntityStore> ref,
      ExecutionSupport executionSupport,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    super.execute(ref, executionSupport, sensorInfo, deltaTime, store);
    WorkplaceAreaSnapshot workplaceSnapshot = this.getAreaSnapshot(ref, store);
    if (workplaceSnapshot == null) {
      return false;
    }

    Vec3i blockPos = workplaceSnapshot.pollNextSnapshotTargetBlockFrom(FarmerScan.TILLABLE_TERRAIN);
    World world = store.getExternalData().getWorld();

    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));
    boolean soilTilled = chunk.setBlock(blockPos.x, blockPos.y, blockPos.z, TILL_BLOCK_TYPE);

    if (soilTilled) {
      NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
      VillagerToolAnimationHelper.playHeldToolAnimation(ref, npcEntity, store, "Till");
      Vec3f center = Utils.centerOf(blockPos);
      SoundUtil.playSoundEvent3d(ref, TILL_SOUND_EVENT_INDEX, center.x, center.y, center.z, store);

      InventoryComponent.Hotbar hotbar = VillagerInventoryAccess.hotbar(store, ref);
      if (hotbar == null || hotbar.getActiveSlot() < 0) {
        return false;
      }

      ItemStack holdingItem = hotbar.getActiveItem();
      if (holdingItem == null) {
        return false;
      }

      ItemStack damagedItem = holdingItem.withDurability(holdingItem.getDurability() - 1);
      hotbar.getInventory().setItemStackForSlot(hotbar.getActiveSlot(), damagedItem);

      if (damagedItem.isBroken()) {
        InventoryHelper.removeItemInHand(ref, store, 1);
      }
      return true;
    }

    return false;
  }
}
