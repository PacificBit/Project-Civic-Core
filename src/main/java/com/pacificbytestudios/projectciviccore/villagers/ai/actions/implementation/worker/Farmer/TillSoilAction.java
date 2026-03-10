package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.Farmer;

import java.util.ArrayDeque;
import java.util.Map;

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
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;

public class TillSoilAction extends ActionBase {
  private static final String[] TILLABLE_SOIL = new String[] { "Soil_Grass", "Soil_Mud_Dry" };
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
      ProjectCivicCore.LOGGER.atSevere().log("[TillSoilAction] Could not fetch villager component");
      return false;
    }

    Map<String, ArrayDeque<Vector3i>> blockPositions = villager
        .getWorkerData()
        .getWorkareaSnapshot()
        .getBlockTypePosition();

    ArrayDeque<Vector3i> grassQueue = blockPositions.get(TILLABLE_SOIL[0]);
    ArrayDeque<Vector3i> mudQueue = blockPositions.get(TILLABLE_SOIL[1]);

    Vector3i blockPos;

    if (grassQueue != null && !grassQueue.isEmpty()) {
      blockPos = grassQueue.pollFirst();
    } else if (mudQueue != null && !mudQueue.isEmpty()) {
      blockPos = mudQueue.pollFirst();
    } else {
      throw new IllegalStateException("No tillable soil block available");
    }

    World world = store.getExternalData().getWorld();

    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(blockPos.x, blockPos.z));
    chunk.setBlock(blockPos.x, blockPos.y, blockPos.z, TILL_BLOCK_TYPE);

    return false;
  }
}
