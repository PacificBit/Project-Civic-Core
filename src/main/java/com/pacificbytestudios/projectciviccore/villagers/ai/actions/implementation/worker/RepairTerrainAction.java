package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker;

import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.BlockPlacementHelper;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.BuilderRepairTerrainAction;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.IWorkAreaScanner;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class RepairTerrainAction extends VillagerActionBase {
  private static final int MAX_DEDUCTION_RADIUS = 3;
  private static final String FALLBACK_REPAIR_BLOCK_ID = "Rock_Stone";
  private static final int MIN_WORLD_Y = 0;
  private static final int MAX_WORLD_Y = 319;

  private final String[] repairBlockSet;

  public RepairTerrainAction(BuilderRepairTerrainAction builderActionBase, BuilderSupport support) {
    super(builderActionBase);
    this.repairBlockSet = builderActionBase.getTerrainRepairBlockSet(support);
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
    if (snapshot == null) {
      return false;
    }

    ArrayDeque<Vec3i> terrainToFill = snapshot.getSnapshotTargets().get(IWorkAreaScanner.TERRAIN_BLOCK_TO_FILL);
    if (terrainToFill == null || terrainToFill.isEmpty()) {
      snapshot.getSnapshotTargets().remove(IWorkAreaScanner.TERRAIN_BLOCK_TO_FILL);
      return false;
    }

    World world = this.getWorld(store);
    boolean repairedAllBlocks = true;

    for (Vec3i blockPos : terrainToFill) {
      repairedAllBlocks &= this.repairTerrainBlock(world, blockPos);
    }

    if (repairedAllBlocks) {
      snapshot.getSnapshotTargets().remove(IWorkAreaScanner.TERRAIN_BLOCK_TO_FILL);
    }

    return repairedAllBlocks;
  }

  private boolean repairTerrainBlock(World world, Vec3i blockPos) {
    if (blockPos == null) {
      Logger.logWarning("Snapshot contains a null terrain position");
      return false;
    }

    WorldChunk chunk = Utils.getWorldChunk(world, blockPos);
    if (chunk == null) {
      Logger.logWarning("Could not load chunk for " + blockPos);
      return false;
    }

    BlockType existingBlock = Utils.getBlockTypeChunkAware(world, chunk, blockPos);
    if (this.isValidSupportBlock(world, blockPos, existingBlock)) {
      return true;
    }

    String blockId = this.getBlockIdToPlace(world, blockPos);
    if (blockId == null || blockId.isEmpty()) {
      Logger.logWarning("Could not resolve repair block id for " + blockPos);
      return false;
    }

    BlockType repairBlock = BlockType.getAssetMap().getAsset(blockId);
    if (repairBlock == null) {
      Logger.logWarning("Unknown repair block id " + blockId + " for " + blockPos);
      return false;
    }

    boolean repaired = chunk.setBlock(blockPos.x, blockPos.y, blockPos.z, repairBlock);
    if (!repaired) {
      Logger.logWarning("Failed to place " + blockId + " at " + blockPos);
    }

    return repaired;
  }

  private boolean isValidSupportBlock(World world, Vec3i blockPos, BlockType blockType) {
    if (blockType == null || blockType.getMaterial() != BlockMaterial.Solid) {
      return false;
    }

    BlockSection blockSection = this.getBlockSection(world, blockPos);
    if (blockSection == null) {
      return true;
    }

    int rotation = blockSection.getRotationIndex(blockPos.x, blockPos.y, blockPos.z);
    int filler = blockSection.getFiller(blockPos.x, blockPos.y, blockPos.z);
    return BlockPlacementHelper.testSupportingBlock(blockType, rotation, filler);
  }

  private String getBlockIdToPlace(World world, Vec3i blockPos) {
    if (this.repairBlockSet == null || this.repairBlockSet.length == 0 || this.repairBlockSet[0].equals("*")) {
      return this.getBlockIdFromNeighbour(world, blockPos);
    }

    int index = ThreadLocalRandom.current().nextInt(this.repairBlockSet.length);
    return this.repairBlockSet[index];
  }

  private String getBlockIdFromNeighbour(World world, Vec3i blockPos) {
    for (int radius = 1; radius <= MAX_DEDUCTION_RADIUS; radius++) {
      for (int xOffset = -radius; xOffset <= radius; xOffset++) {
        for (int zOffset = -radius; zOffset <= radius; zOffset++) {
          if (Math.max(Math.abs(xOffset), Math.abs(zOffset)) != radius) {
            continue;
          }

          Vec3i neighbourPos = Vec3i.from(blockPos).add(xOffset, 0, zOffset);
          WorldChunk neighbourChunk = Utils.getWorldChunk(world, neighbourPos);
          if (neighbourChunk == null) {
            continue;
          }

          BlockType neighbourBlock = Utils.getBlockTypeChunkAware(world, neighbourChunk, neighbourPos);
          if (neighbourBlock == null || neighbourBlock.getMaterial() != BlockMaterial.Solid) {
            continue;
          }

          return neighbourBlock.getId();
        }
      }
    }

    return FALLBACK_REPAIR_BLOCK_ID;
  }

  private BlockSection getBlockSection(World world, Vec3i position) {
    if (world == null || position == null || position.y < MIN_WORLD_Y || position.y > MAX_WORLD_Y) {
      return null;
    }

    Ref<ChunkStore> sectionRef = world.getChunkStore().getChunkSectionReferenceAtBlock(
        position.x,
        position.y,
        position.z);
    if (sectionRef == null || !sectionRef.isValid()) {
      return null;
    }

    return sectionRef.getStore().getComponent(sectionRef, BlockSection.getComponentType());
  }
}
