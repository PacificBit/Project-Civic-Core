package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.miner;

import java.util.ArrayDeque;
import java.util.UUID;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineshaftGallery;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.MinerScan;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class ScanBreakableAreaSensor extends VillagerSensorBase {

  public ScanBreakableAreaSensor(BuilderSensorBase builderSensorBase) {
    super(builderSensorBase);
  }

  @Override
  public InfoProvider getSensorInfo() {
    return null;
  }

  @Override
  public boolean matches(
      Ref<EntityStore> ref,
      ExecutionSupport executionSupport,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.matches(ref, executionSupport, deltaTime, store)) {
      return false;
    }
    this.triggered = true;

    WorkplaceAreaSnapshot snapshot = this.getAreaSnapshot(ref, store);
    UUID villagerId = this.getVillagerId(ref, store);
    MineComponent mineComponent = this.getComponentFromWorkplace(store, ref, MineComponent.getComponentType());
    if (snapshot == null || villagerId == null || mineComponent == null) {
      return false;
    }

    if (!snapshot.isGroupEmtpy(MinerScan.BREAKABLE_BLOCK)) {
      return false;
    }

    Vec3i nextPosition = snapshot.peekNextSnapshotTargetBlockFrom(MinerScan.DIG_POSITIONS);
    if (nextPosition == null) {
      snapshot.removeSnapshotGroup(MinerScan.BREAKABLE_BLOCK);
      Logger.logInfo("No more dig position are present");
      return false;
    }

    MineshaftGallery mineshaftGallery = mineComponent.getWorkerAllocatedGallery(villagerId);
    if (mineshaftGallery == null) {
      Logger.logWarning("Could not find an allocated gallery");
      return false;
    }

    Vec3i cardinalDirection = mineshaftGallery.getCardinalDirection();
    if (cardinalDirection == null) {
      return false;
    }

    ArrayDeque<Vec3i> breakableBlocks = new ArrayDeque<>();

    Vec3i left = Utils.rotateNormalizedVector(cardinalDirection, Rotation.Ninety);
    Vec3i right = Utils.rotateNormalizedVector(cardinalDirection, Rotation.TwoSeventy);

    Vec3i position = Vec3i.from(nextPosition)
        .add(cardinalDirection)
        .add(left)
        .add(Vec3i.safeUp());

    Vec3i size3i = StructureConfig.Mine.MineshaftGalleryConf.SIZE_3I;
    World world = this.getWorld(store);

    for (int y = 0; y < size3i.y; y++) {
      for (int x = 0; x < size3i.x; x++) {
        WorldChunk chunk = Utils.getWorldChunk(world, position);
        if (chunk != null) {
          BlockType blockType = Utils.getBlockTypeChunkAware(world, chunk, position);
          if (blockType != null && blockType.getMaterial() == BlockMaterial.Solid) {
            BlockGathering gathering = blockType.getGathering();
            if (gathering != null) {
              breakableBlocks.addLast(Vec3i.from(position));
            }
          }
        }

        position.add(right);
      }
      position.sub(Vec3i.from(right).mul(size3i.x)).add(Vec3i.safeUp());
    }

    snapshot.removeSnapshotGroup(MinerScan.BREAKABLE_BLOCK);
    snapshot.addAllBlockSnapshotTargets(MinerScan.BREAKABLE_BLOCK, breakableBlocks);
    return !breakableBlocks.isEmpty();
  }
}
