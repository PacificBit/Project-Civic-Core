package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.Farmer;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;

public class TillSoilAction extends ActionBase {
  private static final String TILLABLE_SOIL = "Soil_Grass";
  private static final String TILLED_SOIL = "Soil_Dirt_Tilled";
  private static final BlockType TILL_BLOCK_TYPE = BlockType.getAssetMap().getAsset(TILLED_SOIL);

  public TillSoilAction(BuilderActionBase builderActionBase) {
    super(builderActionBase);
  }

  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    super.execute(ref, role, sensorInfo, deltaTime, store);

    VillagerComponent villager = store.getComponent(ref, VillagerComponent.getComponentType());

    if (villager == null) {
      return false;
    }

    Vector3i blockPos = villager
        .getWorkerData()
        .getWorkareaSnapshot()
        .getBlockTypePosition()
        .get(TILLABLE_SOIL)
        .pollFirst();

    World world = store.getExternalData().getWorld();

    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));
    chunk.setBlock(blockPos.x, blockPos.y, blockPos.z, TILL_BLOCK_TYPE);
    System.out.println("Tilling block: " + blockPos);
    System.out.println("Remaining: "
        + villager.getWorkerData().getWorkareaSnapshot().getBlockTypePosition().get(TILLABLE_SOIL).size());
    System.out.println(villager.getWorkerData().getWorkareaSnapshot().getBlockTypePosition().get(TILLABLE_SOIL));

    return false;
  }
}
