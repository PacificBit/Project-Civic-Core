package com.pacificbytestudios.projectciviccore.villagers.components.workplace;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.pacificbytestudios.projectciviccore.codecs.ArrayDequeCodec;

public class WorkplaceAreaSnapshot {
  public static final Duration EXPIRATION_AMOUNT = Duration.ofHours(1);

  public static final BuilderCodec<WorkplaceAreaSnapshot> CODEC = BuilderCodec
      .builder(WorkplaceAreaSnapshot.class, WorkplaceAreaSnapshot::new)

      .append(
          new KeyedCodec<>(
              "BlockTypePosition",
              new MapCodec<ArrayDeque<Vector3i>, HashMap<String, ArrayDeque<Vector3i>>>(
                  new ArrayDequeCodec<>(
                      Vector3i.CODEC,
                      "Vector3iDeque"),
                  HashMap::new)),
          (obj, value) -> {
            obj.blockTypePosition = new HashMap<>();
            value.forEach((k, v) -> obj.blockTypePosition.put(
                k,
                v == null ? new ArrayDeque<>() : new ArrayDeque<>(v)));
          },
          obj -> {
            HashMap<String, ArrayDeque<Vector3i>> encoded = new HashMap<>();
            obj.blockTypePosition.forEach((k, v) -> encoded.put(
                k,
                v == null ? new ArrayDeque<>() : new ArrayDeque<>(v)));
            return encoded;
          })
      .add()

      .append(
          new KeyedCodec<>("CapturedAt", Codec.INSTANT),
          (obj, value) -> obj.capturedAt = value,
          obj -> obj.capturedAt)
      .add()

      // WARN: With complete i mean the scan is complete
      .append(
          new KeyedCodec<>("Completed", Codec.BOOLEAN),
          (obj, value) -> obj.completed = value,
          obj -> obj.completed)
      .add()

      .append(
          new KeyedCodec<>("LastVisited", Vector3i.CODEC),
          (obj, value) -> obj.lastVisited = value,
          obj -> obj.lastVisited)
      .add()

      .build();

  private Map<String, ArrayDeque<Vector3i>> blockTypePosition = new HashMap<>();
  private Instant capturedAt;
  private boolean completed = false;
  private Vector3i lastVisited;

  public Map<String, ArrayDeque<Vector3i>> getBlockTypePosition() {
    return this.blockTypePosition;
  }

  public void setBlockTypePosition(Map<String, ArrayDeque<Vector3i>> blockTypePosition) {
    this.blockTypePosition = blockTypePosition;
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

  public Vector3i getLastVisited() {
    return this.lastVisited;
  }

  public void setLastVisited(Vector3i lastVisited) {
    this.lastVisited = lastVisited;
  }

  public boolean isValid(WorldTimeResource worldTimeResource) {
    boolean isExpired = isExpired(worldTimeResource);
    boolean isSnapshotTook = this.completed;
    boolean hasGroupNotDoneOperatingOver = this.hasGroupToFinish();
    return !isExpired && isSnapshotTook && hasGroupNotDoneOperatingOver;
  }

  public boolean hasGroupToFinish() {
    for (Map.Entry<String, ArrayDeque<Vector3i>> group : this.blockTypePosition.entrySet()) {
      if (group.getValue().size() > 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public WorkplaceAreaSnapshot clone() {
    WorkplaceAreaSnapshot snapshot = new WorkplaceAreaSnapshot();

    Map<String, ArrayDeque<Vector3i>> clonedMap = new HashMap<>();
    this.blockTypePosition.forEach((key, value) -> clonedMap.put(
        key,
        value == null ? new ArrayDeque<>() : new ArrayDeque<>(value)));

    snapshot.blockTypePosition = clonedMap;
    snapshot.capturedAt = this.capturedAt;
    snapshot.completed = this.completed;
    snapshot.lastVisited = this.lastVisited;
    return snapshot;
  }

  @Override
  public String toString() {
    return "WorkplaceAreaSnapshot{"
        + "blockTypePosition=" + this.blockTypePosition
        + ", capturedAt=" + this.capturedAt
        + ", completed=" + this.completed
        + ", lastVisited=" + this.lastVisited
        + '}';
  }
}
