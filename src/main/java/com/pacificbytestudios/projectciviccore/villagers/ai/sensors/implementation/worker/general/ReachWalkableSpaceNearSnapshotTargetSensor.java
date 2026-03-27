package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general;

import java.util.ArrayDeque;
import java.util.Map;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;
import com.hypixel.hytale.server.npc.util.BlockPlacementHelper;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderReachWalkableSpaceNearSnapshotTargetSensor;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerSensorBase;

public class ReachWalkableSpaceNearSnapshotTargetSensor extends VillagerSensorBase {
  private static final int MIN_WORLD_Y = 0;
  private static final int MAX_WORLD_Y = 319;

  private final PositionProvider positionProvider = new PositionProvider();
  private final String[] blockIds;
  private final double stoppingRangeSquared;
  private final boolean useCenterOfBlock;

  public ReachWalkableSpaceNearSnapshotTargetSensor(
      BuilderReachWalkableSpaceNearSnapshotTargetSensor builderSensorBase,
      BuilderSupport support) {
    super(builderSensorBase);
    this.blockIds = builderSensorBase.getBlockIds(support);

    double stoppingRange = builderSensorBase.getStoppingRange(support);
    this.stoppingRangeSquared = stoppingRange * stoppingRange;
    this.useCenterOfBlock = builderSensorBase.getUseCenterOfBlock(support);
  }

  @Override
  public InfoProvider getSensorInfo() {
    return this.positionProvider;
  }

  @Override
  public boolean matches(
      Ref<EntityStore> ref,
      Role role,
      double dt,
      Store<EntityStore> store) {
    if (!super.matches(ref, role, dt, store)) {
      this.positionProvider.clear();
      return false;
    }

    WorkplaceAreaSnapshot snapshot = this.getAreaSnapshot(ref, store);
    if (snapshot == null) {
      this.positionProvider.clear();
      return false;
    }

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      ProjectCivicCore.LOGGER.atSevere().log(
          "[ReachWalkableSpaceNearSnapshotTargetSensor] Could not find transform component");
      this.positionProvider.clear();
      return false;
    }

    Vector3i target = this.findSnapshotTarget(snapshot.getBlockTypePosition());
    if (target == null) {
      this.positionProvider.clear();
      return false;
    }

    World world = store.getExternalData().getWorld();
    Vector3i walkableTarget = this.findWalkableTarget(world, target, transform.getPosition());
    if (walkableTarget == null) {
      this.positionProvider.clear();
      return false;
    }

    Vector3d targetPosition = this.useCenterOfBlock
        ? Utils.centerOf(walkableTarget).toVector3d()
        : walkableTarget.toVector3d();

    double distanceFromTarget = transform.getPosition().distanceSquaredTo(targetPosition);
    if (distanceFromTarget <= this.stoppingRangeSquared) {
      this.positionProvider.clear();
      return false;
    }

    this.positionProvider.setTarget(targetPosition);
    return true;
  }

  private Vector3i findSnapshotTarget(Map<String, ArrayDeque<Vector3i>> blockPositions) {
    for (String blockId : this.blockIds) {
      ArrayDeque<Vector3i> queue = blockPositions.get(blockId);
      if (queue != null && !queue.isEmpty()) {
        return queue.peekFirst();
      }
    }
    return null;
  }

  private Vector3i findWalkableTarget(World world, Vector3i blockTarget, Vector3d villagerPosition) {
    Vector3i bestCandidate = null;
    double bestDistanceSquared = Double.MAX_VALUE;
    int bestPriority = Integer.MAX_VALUE;

    for (Vector3i direction : Vector3i.CARDINAL_DIRECTIONS) {
      Vector3i candidate = blockTarget.clone().add(direction);
      if (!this.isStandableCandidate(world, candidate)) {
        continue;
      }

      Vector3d candidatePosition = this.useCenterOfBlock
          ? Utils.centerOf(candidate).toVector3d()
          : candidate.toVector3d();

      double distanceSquared = villagerPosition.distanceSquaredTo(candidatePosition);
      int directionPriority = this.getDirectionPriority(direction);
      if (distanceSquared < bestDistanceSquared
          || (distanceSquared == bestDistanceSquared && directionPriority < bestPriority)) {
        bestDistanceSquared = distanceSquared;
        bestPriority = directionPriority;
        bestCandidate = candidate;
      }
    }

    return bestCandidate;
  }

  private boolean isStandableCandidate(World world, Vector3i feetPosition) {
    WorldChunk chunk = Utils.getWorldChunk(world, feetPosition);
    if (chunk == null) {
      return false;
    }

    if (feetPosition.y <= MIN_WORLD_Y || feetPosition.y >= MAX_WORLD_Y) {
      return false;
    }

    Vector3i headPosition = feetPosition.clone().add(0, 1, 0);
    Vector3i supportPosition = feetPosition.clone().add(0, -1, 0);

    return this.isOpenNonFluidSpace(world, feetPosition)
        && this.isOpenNonFluidSpace(world, headPosition)
        && this.isFullSupportBlock(world, supportPosition);
  }

  private boolean isOpenNonFluidSpace(World world, Vector3i position) {
    if (this.getFluidId(world, position) != 0) {
      return false;
    }

    WorldChunk chunk = Utils.getWorldChunk(world, position);
    if (chunk == null) {
      return false;
    }

    BlockType blockType = Utils.getBlockTypeChunkAware(world, chunk, position);
    return blockType != null && blockType.getMaterial() != BlockMaterial.Solid;
  }

  private boolean isFullSupportBlock(World world, Vector3i position) {
    WorldChunk chunk = Utils.getWorldChunk(world, position);
    if (chunk == null) {
      return false;
    }

    BlockType blockType = Utils.getBlockTypeChunkAware(world, chunk, position);
    if (blockType == null) {
      return false;
    }

    BlockSection blockSection = this.getBlockSection(world, position);
    if (blockSection == null) {
      return false;
    }

    int rotation = blockSection.getRotationIndex(position.x, position.y, position.z);
    int filler = blockSection.getFiller(position.x, position.y, position.z);
    return BlockPlacementHelper.testSupportingBlock(blockType, rotation, filler);
  }

  private int getDirectionPriority(Vector3i direction) {
    if (direction == Vector3i.NORTH) {
      return 0;
    }
    if (direction == Vector3i.EAST) {
      return 1;
    }
    if (direction == Vector3i.SOUTH) {
      return 2;
    }
    return 3;
  }

  private int getFluidId(World world, Vector3i position) {
    Ref<ChunkStore> sectionRef = this.getSectionReference(world, position);
    if (sectionRef == null || !sectionRef.isValid()) {
      return 0;
    }

    Store<ChunkStore> sectionStore = sectionRef.getStore();
    FluidSection fluidSection = sectionStore.getComponent(sectionRef, FluidSection.getComponentType());
    if (fluidSection == null) {
      return 0;
    }

    return fluidSection.getFluidId(position.x, position.y, position.z);
  }

  private BlockSection getBlockSection(World world, Vector3i position) {
    Ref<ChunkStore> sectionRef = this.getSectionReference(world, position);
    if (sectionRef == null || !sectionRef.isValid()) {
      return null;
    }

    return sectionRef.getStore().getComponent(sectionRef, BlockSection.getComponentType());
  }

  private Ref<ChunkStore> getSectionReference(World world, Vector3i position) {
    if (position.y < MIN_WORLD_Y || position.y > MAX_WORLD_Y) {
      return null;
    }
    return world.getChunkStore().getChunkSectionReferenceAtBlock(position.x, position.y, position.z);
  }
}
