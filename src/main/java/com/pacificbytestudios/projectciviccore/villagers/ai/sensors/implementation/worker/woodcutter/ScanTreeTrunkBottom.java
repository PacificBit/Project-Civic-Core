package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.woodcutter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.joml.Vector3i;

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
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.WoodCutterScan;

public class ScanTreeTrunkBottom extends VillagerSensorBase {
  private static final int MAX_SCAN_RANGE = 3;

  private final PositionProvider positionProvider = new PositionProvider();
  private Vector3i centerTreeBlock;
  private Set<Vector3i> treeTrunkBottomBlocks;
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

      ArrayDeque<Vector3i> treeTargets = snapshot.getBlockTypePosition().get(WoodCutterScan.CUTTABLE_TREE);
      if (treeTargets == null || treeTargets.isEmpty()) {
        ProjectCivicCore.LOGGER.atFine().log("[FindNextTrunkToCutSensor] No cuttable tree target is available");
        return false;
      }

      this.centerTreeBlock = treeTargets.peekFirst();
      if (this.centerTreeBlock == null) {
        ProjectCivicCore.LOGGER.atFine().log("[FindNextTrunkToCutSensor] Cuttable tree target was null");
        return false;
      }

      this.world = store.getExternalData().getWorld();
      this.chunk = Utils.getWorldChunk(this.world, this.centerTreeBlock);
      if (this.chunk == null) {
        ProjectCivicCore.LOGGER.atFine().log("[FindNextTrunkToCutSensor] Could not load chunk for " +
            this.centerTreeBlock);
        return false;
      }

      this.scanTreeTrunk(this.centerTreeBlock);

      if (treeTrunkBottomBlocks.isEmpty()) {
        treeTargets.pollFirst();
        ProjectCivicCore.LOGGER.atFine().log("[FindNextTrunkToCutSensor] Started cutting tree at: " +
            this.centerTreeBlock + ", but no tree was found");
        return false;
      }

      List<Vector3i> orderedTargets = new ArrayList<>(this.treeTrunkBottomBlocks);
      Collections.reverse(orderedTargets);

      ArrayDeque<Vector3i> targets = new ArrayDeque<>(orderedTargets);
      snapshot.getBlockTypePosition().put(WoodCutterScan.CUTTABLE_BLOCK, targets);
      return true;
    }

    return false;
  }

  private void scanTreeTrunk(Vector3i origin) {
    BlockType treeTrunk = Utils.getBlockTypeChunkAware(this.world, this.chunk, origin);
    ProjectCivicCore.LOGGER.atInfo().log("[scanTreeTrunk] Started new tree scan");
    if (treeTrunk == null || treeTrunk.getId() == null) {
      return;
    }
    String trunkBlockId = treeTrunk.getId();
    this.treeTrunkBottomBlocks.add(VectorUtil.clone(origin));

    boolean foundSomething = true;
    int range = 1;

    while (foundSomething && range <= MAX_SCAN_RANGE) {
      foundSomething = false;

      Vector3i topLeft = VectorUtil.clone(origin)
          .add(VectorUtil.NORTH().mul(range))
          .add(VectorUtil.WEST().mul(range));

      Vector3i botRigth = VectorUtil.clone(origin)
          .add(VectorUtil.EAST().mul(range))
          .add(VectorUtil.SOUTH().mul(range));

      foundSomething |= scanLine(topLeft, VectorUtil.SOUTH(), range * 2 + 1, trunkBlockId);
      foundSomething |= scanLine(topLeft, VectorUtil.EAST(), range * 2 + 1, trunkBlockId);
      foundSomething |= scanLine(botRigth, VectorUtil.WEST(), range * 2 + 1, trunkBlockId);
      foundSomething |= scanLine(botRigth, VectorUtil.NORTH(), range * 2 + 1, trunkBlockId);

      range += 1;
    }
  }

  private boolean scanLine(Vector3i startPoint, Vector3i direction, int count, String blockId) {
    Vector3i position = VectorUtil.clone(startPoint);
    boolean foundSomething = false;

    for (int i = 0; i < count; i++) {
      BlockType blockType = Utils.getBlockTypeChunkAware(this.world, this.chunk, position);

      if (blockType == null) {
        position.add(direction);
        continue;
      }

      if (blockType.getId().equals(blockId) || blockType.getId().startsWith(blockId)) {
        this.treeTrunkBottomBlocks.add(VectorUtil.clone(position));
        foundSomething = true;
      }

      position.add(direction);
    }
    return foundSomething;
  }
}
