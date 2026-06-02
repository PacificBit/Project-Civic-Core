package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.woodcutter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.WoodCutterScan;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class ScanTreeTrunkBottom extends VillagerSensorBase {
  private static final int MAX_SCAN_RANGE = 3;

  private final PositionProvider positionProvider = new PositionProvider();
  private Vec3i centerTreeBlock;
  private Set<Vec3i> treeTrunkBottomBlocks;
  private World world;
  private WorldChunk chunk;

  public ScanTreeTrunkBottom(BuilderSensorBase builderSensorBase) {
    super(builderSensorBase);
  }

  @Override
  public InfoProvider getSensorInfo() {
    return positionProvider;
  }

  @Override
  public boolean matches(
      Ref<EntityStore> ref,
      Role role,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.matches(ref, role, deltaTime, store)) {
      return false;
    }

    WorkplaceAreaSnapshot snapshot = this.getAreaSnapshot(ref, store);
    if (snapshot == null) {
      return false;
    }

    if (!this.isTriggered()) {
      this.triggered = true;
      this.treeTrunkBottomBlocks = new LinkedHashSet<>();
      snapshot.getBlockTypePosition().remove(WoodCutterScan.CUTTABLE_BLOCK);

      ArrayDeque<Vec3i> treeTargets = snapshot.getBlockTypePosition().get(WoodCutterScan.CUTTABLE_TREE);
      if (treeTargets == null || treeTargets.isEmpty()) {
        Logger.logFine("No cuttable tree target is available");
        return false;
      }

      this.centerTreeBlock = treeTargets.peekFirst();
      if (this.centerTreeBlock == null) {
        Logger.logFine("Cuttable tree target was null");
        return false;
      }

      this.world = store.getExternalData().getWorld();
      this.chunk = Utils.getWorldChunk(this.world, this.centerTreeBlock);
      if (this.chunk == null) {
        Logger.logFine("Could not load chunk for " +
            this.centerTreeBlock);
        return false;
      }

      this.scanTreeTrunk(this.centerTreeBlock);

      if (treeTrunkBottomBlocks.isEmpty()) {
        treeTargets.pollFirst();
        Logger.logFine("Started cutting tree at: " +
            this.centerTreeBlock + ", but no tree was found");
        return false;
      }

      List<Vec3i> orderedTargets = new ArrayList<>(this.treeTrunkBottomBlocks);
      Collections.reverse(orderedTargets);

      ArrayDeque<Vec3i> targets = new ArrayDeque<>(orderedTargets);
      snapshot.getBlockTypePosition().put(WoodCutterScan.CUTTABLE_BLOCK, targets);
      return true;
    }

    return false;
  }

  private void scanTreeTrunk(Vec3i origin) {
    BlockType treeTrunk = Utils.getBlockTypeChunkAware(this.world, this.chunk, origin);
    Logger.logInfo("Started new tree scan");
    if (treeTrunk == null || treeTrunk.getId() == null) {
      return;
    }
    String trunkBlockId = treeTrunk.getId();
    this.treeTrunkBottomBlocks.add(Vec3i.from(origin));

    boolean foundSomething = true;
    int range = 1;

    while (foundSomething && range <= MAX_SCAN_RANGE) {
      foundSomething = false;

      Vec3i topLeft = Vec3i.from(origin)
          .add(Vec3i.safeNorth().mul(range))
          .add(Vec3i.safeWest().mul(range));

      Vec3i botRigth = Vec3i.from(origin)
          .add(Vec3i.safeEast().mul(range))
          .add(Vec3i.safeSouth().mul(range));

      foundSomething |= scanLine(topLeft, Vec3i.safeSouth(), range * 2 + 1, trunkBlockId);
      foundSomething |= scanLine(topLeft, Vec3i.safeEast(), range * 2 + 1, trunkBlockId);
      foundSomething |= scanLine(botRigth, Vec3i.safeWest(), range * 2 + 1, trunkBlockId);
      foundSomething |= scanLine(botRigth, Vec3i.safeNorth(), range * 2 + 1, trunkBlockId);

      range += 1;
    }
  }

  private boolean scanLine(Vec3i startPoint, Vec3i direction, int count, String blockId) {
    Vec3i position = Vec3i.from(startPoint);
    boolean foundSomething = false;

    for (int i = 0; i < count; i++) {
      BlockType blockType = Utils.getBlockTypeChunkAware(this.world, this.chunk, position);

      if (blockType == null) {
        position.add(direction);
        continue;
      }

      if (blockType.getId().equals(blockId) || blockType.getId().startsWith(blockId)) {
        this.treeTrunkBottomBlocks.add(Vec3i.from(position));
        foundSomething = true;
      }

      position.add(direction);
    }
    return foundSomething;
  }
}
