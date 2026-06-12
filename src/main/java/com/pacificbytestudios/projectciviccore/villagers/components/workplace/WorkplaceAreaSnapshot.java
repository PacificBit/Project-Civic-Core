package com.pacificbytestudios.projectciviccore.villagers.components.workplace;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.pacificbytestudio.codecbutler.annotations.SerialDescription;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerialWithCodec;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

@SerializableClass
public class WorkplaceAreaSnapshot {
  public static final Duration EXPIRATION_AMOUNT = Duration.ofHours(1);

  public static final BuilderCodec<WorkplaceAreaSnapshot> CODEC = WorkplaceAreaSnapshotCodec.CODEC;

  @SerialName("Vector3iDeque")
  Map<String, ArrayDeque<BlockTarget>> snapshotTargets = new HashMap<>();

  @SerialWithCodec("com.hypixel.hytale.codec.Codec.INSTANT")
  Instant capturedAt;

  @SerialDescription("Used to indicate whether or not the workplace area snapshot was comcluded")
  boolean completed = false;
  Vec3i lastVisited;

  public void addWorkplaceSnapshotTarget(String group, Vec3i targetBlock) {
    this.addWorkplaceSnapshotTarget(group, new BlockTarget(targetBlock));
  }

  public void addWorkplaceSnapshotTarget(String group, BlockTarget target) {
    this.snapshotTargets.computeIfAbsent(group, ignored -> new ArrayDeque<>()).addLast(target);
  }

  public void addAllWorkplaceSnapshotTargets(String group, BlockTarget... targets) {
    Collections.addAll(
        this.snapshotTargets.computeIfAbsent(group, ignored -> new ArrayDeque<>()),
        targets);
  }

  public void addAllWorkplaceSnapshotTargets(String group, ArrayDeque<BlockTarget> targets) {
    this.snapshotTargets.put(group, targets);
  }

  public ArrayDeque<BlockTarget> getSnapshotFrom(String group) {
    return this.snapshotTargets.get(group);
  }

  public BlockTarget peekNextSnapshotTargetFrom(String group) {
    ArrayDeque<BlockTarget> targets = this.snapshotTargets.get(group);
    return targets == null ? null : targets.peek();
  }

  public BlockTarget pollNextSnapshotTargetFrom(String group) {
    ArrayDeque<BlockTarget> targets = this.snapshotTargets.get(group);
    return targets == null ? null : targets.poll();
  }

  public void removeSnapshotGroup(String group) {
    this.snapshotTargets.remove(group);
  }

  public void removeAllFromSnapshot(String... groups) {
    Stream.of(groups).forEach(this.snapshotTargets::remove);
  }

  public boolean isGroupEmtpy(String group) {
    ArrayDeque<BlockTarget> targets = this.snapshotTargets.get(group);
    return targets == null ? true : targets.isEmpty();
  }

  public void removeFirstSnapshotTargetFrom(String group) {
    this.pollNextSnapshotTargetFrom(group);
  }

  public void clearSnapshotTargets() {
    this.snapshotTargets.clear();
  }

  public void clearSnapshotGroupTargets(String group) {
    ArrayDeque<BlockTarget> targets = this.snapshotTargets.get(group);

    if (targets != null) {
      targets.clear();
    }
  }

  public Instant getCapturedAt() {
    return this.capturedAt;
  }

  public void captureNow(WorldTimeResource worldTimeResource) {
    this.capturedAt = worldTimeResource.getGameTime();
  }

  public boolean isExpired(WorldTimeResource worldTimeResource) {
    if (this.capturedAt == null) {
      return true;
    }

    if (worldTimeResource == null || worldTimeResource.getGameTime() == null) {
      return true;
    }

    Instant currentGameTime = worldTimeResource.getGameTime();
    return this.capturedAt.plus(EXPIRATION_AMOUNT).isBefore(currentGameTime);
  }

  public boolean isCompleted() {
    return this.completed;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  public Vec3i getLastVisited() {
    return this.lastVisited;
  }

  public void setLastVisited(Vec3i lastVisited) {
    this.lastVisited = lastVisited;
  }

  public boolean isValid(WorldTimeResource worldTimeResource) {
    boolean isExpired = isExpired(worldTimeResource);
    boolean isSnapshotTook = this.completed;
    boolean hasGroupNotDoneOperatingOver = this.hasNonEmptySnapshotTargetGroup();
    return !isExpired && isSnapshotTook && hasGroupNotDoneOperatingOver;
  }

  public boolean hasNonEmptySnapshotTargetGroup() {
    for (Map.Entry<String, ArrayDeque<BlockTarget>> group : this.snapshotTargets.entrySet()) {
      if (group.getValue().size() > 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public WorkplaceAreaSnapshot clone() {
    return WorkplaceAreaSnapshotCodec.clone(this);
  }

  @Override
  public String toString() {
    return "WorkplaceAreaSnapshot{"
        + "\nblockTypePosition=" + this.snapshotTargets
        + ",\n capturedAt=" + this.capturedAt
        + ",\n completed=" + this.completed
        + ",\n lastVisited=" + this.lastVisited
        + '}';
  }
}
