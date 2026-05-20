package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.npc.util.BlockPlacementHelper;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.VectorUtil;

final class SnapshotTargetStandabilityInspector {
  private static final int MIN_WORLD_Y = 0;
  private static final int MAX_WORLD_Y = 319;

  private SnapshotTargetStandabilityInspector() {
  }

  static Inspection inspect(
      World world,
      Vector3i rawTarget,
      Vector3d villagerPosition,
      boolean useCenterOfBlock) {
    Vector3i[] cardinalDirections = VectorUtil.cardinalDirections();
    List<CandidateEvaluation> candidates = new ArrayList<>(cardinalDirections.length);
    Vector3i bestStandableCandidate = null;
    double bestDistanceSquared = Double.MAX_VALUE;
    int bestPriority = Integer.MAX_VALUE;

    for (Vector3i direction : cardinalDirections) {
      Vector3i candidate = VectorUtil.clone(rawTarget).add(direction);
      CandidateEvaluation evaluation = evaluateCandidate(world, candidate, direction, villagerPosition,
          useCenterOfBlock);
      candidates.add(evaluation);
      if (!evaluation.isAccepted()) {
        continue;
      }

      if (evaluation.getDistanceSquared() < bestDistanceSquared
          || (evaluation.getDistanceSquared() == bestDistanceSquared
              && evaluation.getDirectionPriority() < bestPriority)) {
        bestStandableCandidate = candidate;
        bestDistanceSquared = evaluation.getDistanceSquared();
        bestPriority = evaluation.getDirectionPriority();
      }
    }

    return new Inspection(
        bestStandableCandidate,
        bestStandableCandidate == null ? Double.NaN : bestDistanceSquared,
        candidates);
  }

  private static CandidateEvaluation evaluateCandidate(
      World world,
      Vector3i candidate,
      Vector3i direction,
      Vector3d villagerPosition,
      boolean useCenterOfBlock) {
    Vector3d candidatePosition = useCenterOfBlock
        ? VectorUtil.toVector3d(Utils.centerOf(candidate))
        : VectorUtil.toVector3d(candidate);
    double distanceSquared = villagerPosition.distanceSquared(candidatePosition);
    CandidateRejectionReason rejectionReason = determineRejectionReason(world, candidate);
    return new CandidateEvaluation(
        direction,
        candidate,
        distanceSquared,
        rejectionReason,
        getDirectionPriority(direction));
  }

  private static CandidateRejectionReason determineRejectionReason(World world, Vector3i feetPosition) {
    if (world == null || feetPosition == null || feetPosition.y <= MIN_WORLD_Y || feetPosition.y >= MAX_WORLD_Y) {
      return CandidateRejectionReason.OUT_OF_BOUNDS;
    }

    Vector3i headPosition = VectorUtil.clone(feetPosition).add(Vector3iUtil.UP);
    Vector3i supportPosition = VectorUtil.clone(feetPosition).add(Vector3iUtil.DOWN);

    if (hasFluidAt(world, feetPosition) || hasFluidAt(world, headPosition)) {
      return CandidateRejectionReason.FLUID;
    }

    if (isSolidBlock(world, feetPosition)) {
      return CandidateRejectionReason.BLOCKED_FEET;
    }

    if (isSolidBlock(world, headPosition)) {
      return CandidateRejectionReason.BLOCKED_HEAD;
    }

    if (!isFullSupportBlock(world, supportPosition)) {
      return CandidateRejectionReason.MISSING_SUPPORT;
    }

    return null;
  }

  private static boolean hasFluidAt(World world, Vector3i position) {
    return getFluidId(world, position) != 0;
  }

  private static boolean isSolidBlock(World world, Vector3i position) {
    WorldChunk chunk = Utils.getWorldChunk(world, position);
    if (chunk == null) {
      return true;
    }

    BlockType blockType = Utils.getBlockTypeChunkAware(world, chunk, position);
    return blockType == null || blockType.getMaterial() == BlockMaterial.Solid;
  }

  private static boolean isFullSupportBlock(World world, Vector3i position) {
    WorldChunk chunk = Utils.getWorldChunk(world, position);
    if (chunk == null) {
      return false;
    }

    BlockType blockType = Utils.getBlockTypeChunkAware(world, chunk, position);
    if (blockType == null) {
      return false;
    }

    BlockSection blockSection = getBlockSection(world, position);
    if (blockSection == null) {
      return false;
    }

    int rotation = blockSection.getRotationIndex(position.x, position.y, position.z);
    int filler = blockSection.getFiller(position.x, position.y, position.z);
    return BlockPlacementHelper.testSupportingBlock(blockType, rotation, filler);
  }

  private static int getFluidId(World world, Vector3i position) {
    Ref<ChunkStore> sectionRef = getSectionReference(world, position);
    if (sectionRef == null || !sectionRef.isValid()) {
      return 0;
    }

    FluidSection fluidSection = sectionRef.getStore().getComponent(sectionRef, FluidSection.getComponentType());
    if (fluidSection == null) {
      return 0;
    }

    return fluidSection.getFluidId(position.x, position.y, position.z);
  }

  private static BlockSection getBlockSection(World world, Vector3i position) {
    Ref<ChunkStore> sectionRef = getSectionReference(world, position);
    if (sectionRef == null || !sectionRef.isValid()) {
      return null;
    }

    return sectionRef.getStore().getComponent(sectionRef, BlockSection.getComponentType());
  }

  private static Ref<ChunkStore> getSectionReference(World world, Vector3i position) {
    if (position.y < MIN_WORLD_Y || position.y > MAX_WORLD_Y) {
      return null;
    }

    return world.getChunkStore().getChunkSectionReferenceAtBlock(position.x, position.y, position.z);
  }

  private static int getDirectionPriority(Vector3i direction) {
    if (direction.equals(VectorUtil.NORTH())) {
      return 0;
    }
    if (direction.equals(VectorUtil.EAST())) {
      return 1;
    }
    if (direction.equals(VectorUtil.SOUTH())) {
      return 2;
    }
    return 3;
  }

  static final class Inspection {
    private final Vector3i bestStandableCandidate;
    private final double bestStandableDistanceSquared;
    private final List<CandidateEvaluation> candidates;

    private Inspection(
        Vector3i bestStandableCandidate,
        double bestStandableDistanceSquared,
        List<CandidateEvaluation> candidates) {
      this.bestStandableCandidate = bestStandableCandidate;
      this.bestStandableDistanceSquared = bestStandableDistanceSquared;
      this.candidates = candidates;
    }

    Vector3i getBestStandableCandidate() {
      return bestStandableCandidate;
    }

    double getBestStandableDistanceSquared() {
      return bestStandableDistanceSquared;
    }

    String formatCandidateReport() {
      StringBuilder builder = new StringBuilder("[");
      for (int i = 0; i < this.candidates.size(); i++) {
        if (i > 0) {
          builder.append("; ");
        }
        builder.append(this.candidates.get(i).formatForDebug());
      }
      builder.append(']');
      return builder.toString();
    }
  }

  private static final class CandidateEvaluation {
    private final Vector3i direction;
    private final Vector3i candidate;
    private final double distanceSquared;
    private final CandidateRejectionReason rejectionReason;
    private final int directionPriority;

    private CandidateEvaluation(
        Vector3i direction,
        Vector3i candidate,
        double distanceSquared,
        CandidateRejectionReason rejectionReason,
        int directionPriority) {
      this.direction = direction;
      this.candidate = candidate;
      this.distanceSquared = distanceSquared;
      this.rejectionReason = rejectionReason;
      this.directionPriority = directionPriority;
    }

    private boolean isAccepted() {
      return this.rejectionReason == null;
    }

    private double getDistanceSquared() {
      return this.distanceSquared;
    }

    private int getDirectionPriority() {
      return this.directionPriority;
    }

    private String formatForDebug() {
      return this.direction
          + "->"
          + this.candidate
          + ' '
          + (this.isAccepted()
              ? "accepted"
              : "rejected reason=" + this.rejectionReason.getLabel())
          + " distSquared="
          + this.distanceSquared;
    }
  }

  private enum CandidateRejectionReason {
    BLOCKED_FEET("blocked_feet"),
    BLOCKED_HEAD("blocked_head"),
    MISSING_SUPPORT("missing_support"),
    FLUID("fluid"),
    OUT_OF_BOUNDS("out_of_bounds");

    private final String label;

    CandidateRejectionReason(String label) {
      this.label = label;
    }

    private String getLabel() {
      return this.label;
    }
  }
}
