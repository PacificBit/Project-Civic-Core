package com.pacificbytestudios.projectciviccore.villagers.core.inventory;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;

public class ContainerResolver {

  public record ResolvedContainer(
      Vec3i basePosition,
      Ref<ChunkStore> blockRef,
      ItemContainerBlock containerBlock) {
  }

  private static BlockSection getBlockSection(World world, Vec3i pos) {
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

  private static Vec3i resolveBaseBlockPosition(World world, Vec3i pos) {
    BlockSection section = getBlockSection(world, pos);
    if (section == null) {
      return null;
    }

    int filler = section.getFiller(pos.x, pos.y, pos.z);
    if (filler == 0) {
      return pos;
    }

    return new Vec3i(
        pos.x - FillerBlockUtil.unpackX(filler),
        pos.y - FillerBlockUtil.unpackY(filler),
        pos.z - FillerBlockUtil.unpackZ(filler));
  }

  private static ResolvedContainer resolveContainerAtExact(World world, Vec3i pos) {
    if (world == null || pos == null) {
      return null;
    }

    Vec3i basePos = resolveBaseBlockPosition(world, pos);
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

  public static ResolvedContainer resolveContainerAt(World world, Vec3i pos) {
    ResolvedContainer resolved = resolveContainerAtExact(world, pos);
    if (resolved != null) {
      return resolved;
    }

    if (pos == null) {
      return null;
    }

    Vec3i[] adjacentPositions = new Vec3i[] {
        Vec3i.from(pos).add(Vec3i.safeNorth()),
        Vec3i.from(pos).add(Vec3i.safeSouth()),
        Vec3i.from(pos).add(Vec3i.safeEast()),
        Vec3i.from(pos).add(Vec3i.safeWest())
    };

    for (Vec3i adjacentPosition : adjacentPositions) {
      resolved = resolveContainerAtExact(world, adjacentPosition);
      if (resolved != null) {
        return resolved;
      }
    }

    return null;
  }

}
