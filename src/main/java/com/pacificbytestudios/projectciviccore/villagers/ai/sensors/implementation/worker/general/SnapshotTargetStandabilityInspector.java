package com.pacificbytestudios.projectciviccore.villagers.ai.sensors.implementation.worker.general;

import java.util.ArrayList;
import java.util.List;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.npc.util.BlockPlacementHelper;
import com.pacificbytestudios.projectciviccore.utils.Utils;

final class SnapshotTargetStandabilityInspector {
  private static final int MIN_WORLD_Y = 0;
  private static final int MAX_WORLD_Y = 319;

  private SnapshotTargetStandabilityInspector() {
  }

  static Inspection inspect(
      World world,
      Vec3i rawTarget,
      Vec3d villagerPosition,
      boolean useCenterOfBlock) {
    Vec3i[] cardinalDirections = Vec3i.cardinalDirections();
    List<CandidateEvaluation> candidates = new ArrayList<>(cardinalDirections.length);
    Vec3i bestStandableCandidate = null;
    double bestDistanceSquared = Double.MAX_VALUE;
    int bestPriority = Integer.MAX_VALUE;

    for (Vec3i direction : cardinalDirections) {
      Vec3i candidate = Vec3i.from(rawTarget).add(direction);
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
      Vec3i candidate,
      Vec3i direction,
      Vec3d villagerPosition,
      boolean useCenterOfBlock) {
    Vec3d candidatePosition = useCenterOfBlock
        ? Vec3d.from(Utils.centerOf(candidate))
        : Vec3d.from(candidate);
    double distanceSquared = villagerPosition.distanceSquared(candidatePosition);
    CandidateRejectionReason rejectionReason = determineRejectionReason(world, candidate);
    return new CandidateEvaluation(
        direction,
        candidate,
        distanceSquared,
        rejectionReason,
        getDirectionPriority(direction));
  }

  private static CandidateRejectionReason determineRejectionReason(World world, Vec3i feetPosition) {
    if (world == null || feetPosition == null || feetPosition.y <= MIN_WORLD_Y || feetPosition.y >= MAX_WORLD_Y) {
      return CandidateRejectionReason.OUT_OF_BOUNDS;
    }

    Vec3i headPosition = Vec3i.from(feetPosition).add(Vec3i.safeUp());
    Vec3i supportPosition = Vec3i.from(feetPosition).add(Vec3i.safeDown());

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

  private static boolean hasFluidAt(World world, Vec3i position) {
    return getFluidId(world, position) != 0;
  }

  private static boolean isSolidBlock(World world, Vec3i position) {
    WorldChunk chunk = Utils.getWorldChunk(world, position);
    if (chunk == null) {
      return true;
    }

    BlockType blockType = Utils.getBlockTypeChunkAware(world, chunk, position);
    return blockType == null || blockType.getMaterial() == BlockMaterial.Solid;
  }

  private static boolean isFullSupportBlock(World world, Vec3i position) {
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

  private static int getFluidId(World world, Vec3i position) {
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

  private static BlockSection getBlockSection(World world, Vec3i position) {
    Ref<ChunkStore> sectionRef = getSectionReference(world, position);
    if (sectionRef == null || !sectionRef.isValid()) {
      return null;
    }

    return sectionRef.getStore().getComponent(sectionRef, BlockSection.getComponentType());
  }

  private static Ref<ChunkStore> getSectionReference(World world, Vec3i position) {
    if (position.y < MIN_WORLD_Y || position.y > MAX_WORLD_Y) {
      return null;
    }

    return world.getChunkStore().getChunkSectionReferenceAtBlock(position.x, position.y, position.z);
  }

  private static int getDirectionPriority(Vec3i direction) {
    if (direction.equals(Vec3i.safeNorth())) {
      return 0;
    }
    if (direction.equals(Vec3i.safeEast())) {
      return 1;
    }
    if (direction.equals(Vec3i.safeSouth())) {
      return 2;
    }
    return 3;
  }

  static final class Inspection {
    private final Vec3i bestStandableCandidate;
    private final double bestStandableDistanceSquared;
    private final List<CandidateEvaluation> candidates;

    private Inspection(
        Vec3i bestStandableCandidate,
        double bestStandableDistanceSquared,
        List<CandidateEvaluation> candidates) {
      this.bestStandableCandidate = bestStandableCandidate;
      this.bestStandableDistanceSquared = bestStandableDistanceSquared;
      this.candidates = candidates;
    }

    Vec3i getBestStandableCandidate() {
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
    private final Vec3i direction;
    private final Vec3i candidate;
    private final double distanceSquared;
    private final CandidateRejectionReason rejectionReason;
    private final int directionPriority;

    private CandidateEvaluation(
        Vec3i direction,
        Vec3i candidate,
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
