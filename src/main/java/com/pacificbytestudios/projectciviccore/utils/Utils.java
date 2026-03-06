package com.pacificbytestudios.projectciviccore.utils;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;

public class Utils {

  public record BlockContext(WorldChunk chunk, Vector3i position) {
  }

  public static BlockContext getWorldBlockContext(
      Ref<ChunkStore> ref,
      CommandBuffer<ChunkStore> commandBuffer) {
    BlockModule.BlockStateInfo info = commandBuffer.getComponent(
        ref,
        BlockModule.BlockStateInfo.getComponentType());

    if (info == null) {
      return null;
    }

    int localX = ChunkUtil.xFromBlockInColumn(info.getIndex());
    int localY = ChunkUtil.yFromBlockInColumn(info.getIndex());
    int localZ = ChunkUtil.zFromBlockInColumn(info.getIndex());

    WorldChunk worldChunk = commandBuffer.getComponent(
        info.getChunkRef(),
        WorldChunk.getComponentType());

    if (worldChunk == null) {
      return null;
    }

    int worldX = ChunkUtil.worldCoordFromLocalCoord(worldChunk.getX(), localX);
    int worldY = localY;
    int worldZ = ChunkUtil.worldCoordFromLocalCoord(worldChunk.getZ(), localZ);

    return new BlockContext(worldChunk, new Vector3i(worldX, worldY, worldZ));
  }

  public static ClaimedAreaComponent claimVillageArea(VillageHeartComponent heart, Vector3i origin) {
    int level = heart.getLevel();
    int radiusPerLevel = heart.getConfig().getClaimRadiusPerLevel();
    int totalRadius = level * radiusPerLevel;
    Vector2i topLeft = new Vector2i(origin.x - totalRadius, origin.z - totalRadius);
    Vector2i botRight = new Vector2i(origin.x + totalRadius, origin.z + totalRadius);
    ProjectCivicCore.LOGGER.atInfo()
        .log("[claimVillageArea] Created new village with anchor points: " + topLeft + " and: " + botRight);
    ClaimedAreaComponent area = new ClaimedAreaComponent();
    area.setTopLeft(topLeft);
    area.setBotRight(botRight);
    heart.setClaimedArea(area);
    return area;
  }
}
