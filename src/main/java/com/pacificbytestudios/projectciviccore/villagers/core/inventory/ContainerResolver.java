package com.pacificbytestudios.projectciviccore.villagers.core.inventory;

import org.joml.Vector3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import com.pacificbytestudios.projectciviccore.utils.VectorUtil;

public class ContainerResolver {

  public record ResolvedContainer(
      Vector3i basePosition,
      Ref<ChunkStore> blockRef,
      ItemContainerBlock containerBlock) {
  }

  private static BlockSection getBlockSection(World world, Vector3i pos) {
    Ref<ChunkStore> sectionRef = world.getChunkStore().getChunkSectionReference(
        ChunkUtil.chunkCoordinate(pos.x),
        ChunkUtil.chunkCoordinate(pos.y),
        ChunkUtil.chunkCoordinate(pos.z));

    if (sectionRef == null || !sectionRef.isValid()) {
      return null;
    }

    Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
    return chunkStore.getComponent(sectionRef, BlockSection.getComponentType());
  }

  private static Vector3i resolveBaseBlockPosition(World world, Vector3i pos) {
    BlockSection section = getBlockSection(world, pos);
    if (section == null) {
      return null;
    }

    int filler = section.getFiller(pos.x, pos.y, pos.z);
    if (filler == 0) {
      return pos;
    }

    return new Vector3i(
        pos.x - FillerBlockUtil.unpackX(filler),
        pos.y - FillerBlockUtil.unpackY(filler),
        pos.z - FillerBlockUtil.unpackZ(filler));
  }

  private static ResolvedContainer resolveContainerAtExact(World world, Vector3i pos) {
    if (world == null || pos == null) {
      return null;
    }

    Vector3i basePos = resolveBaseBlockPosition(world, pos);
    if (basePos == null) {
      return null;
    }

    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(basePos.x, basePos.z));
    if (chunk == null) {
      return null;
    }

    Ref<ChunkStore> blockRef = chunk.getBlockComponentEntity(basePos.x, basePos.y, basePos.z);
    if (blockRef == null || !blockRef.isValid()) {
      return null;
    }

    ItemContainerBlock containerBlock = blockRef.getStore().getComponent(
        blockRef,
        ItemContainerBlock.getComponentType());

    if (containerBlock == null) {
      return null;
    }

    return new ResolvedContainer(basePos, blockRef, containerBlock);
  }

  public static ResolvedContainer resolveContainerAt(World world, Vector3i pos) {
    ResolvedContainer resolved = resolveContainerAtExact(world, pos);
    if (resolved != null) {
      return resolved;
    }

    if (pos == null) {
      return null;
    }

    Vector3i[] adjacentPositions = new Vector3i[] {
        VectorUtil.clone(pos).add(VectorUtil.NORTH()),
        VectorUtil.clone(pos).add(VectorUtil.SOUTH()),
        VectorUtil.clone(pos).add(VectorUtil.EAST()),
        VectorUtil.clone(pos).add(VectorUtil.WEST())
    };

    for (Vector3i adjacentPosition : adjacentPositions) {
      resolved = resolveContainerAtExact(world, adjacentPosition);
      if (resolved != null) {
        return resolved;
      }
    }

    return null;
  }

}
