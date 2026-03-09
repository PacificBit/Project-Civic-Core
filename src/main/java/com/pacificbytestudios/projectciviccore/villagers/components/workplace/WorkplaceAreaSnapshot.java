package com.pacificbytestudios.projectciviccore.villagers.components.workplace;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.math.vector.Vector3i;

public class WorkplaceAreaSnapshot {
  public static final BuilderCodec<WorkplaceAreaSnapshot> CODEC = BuilderCodec
      .builder(WorkplaceAreaSnapshot.class, WorkplaceAreaSnapshot::new)

      .append(
          new KeyedCodec<>(
              "BlockTypePosition",
              new MapCodec<Vector3i, HashMap<String, Vector3i>>(
                  Vector3i.CODEC,
                  HashMap::new)),
          (obj, value) -> {
            obj.blockTypePosition = new HashMap<>();
            value.forEach((k, v) -> obj.blockTypePosition.put(k, v));
          },
          obj -> {
            HashMap<String, Vector3i> encoded = new HashMap<>();
            obj.blockTypePosition.forEach((k, v) -> encoded.put(k, v));
            return encoded;
          })
      .add()

      .append(new KeyedCodec<>("CapturedAt", Codec.INSTANT),
          (obj, value) -> obj.capturedAt = value,
          obj -> obj.capturedAt)
      .add()

      .append(new KeyedCodec<>("Completed", Codec.BOOLEAN),
          (obj, value) -> obj.completed = value,
          obj -> obj.completed)
      .add()

      .build();

  private Map<String, Vector3i> blockTypePosition = new HashMap<>();
  private Instant capturedAt;
  private boolean completed;

  public WorkplaceAreaSnapshot clone() {
    WorkplaceAreaSnapshot snapshot = new WorkplaceAreaSnapshot();
    snapshot.blockTypePosition = new HashMap<>(this.blockTypePosition);
    snapshot.capturedAt = this.capturedAt;
    snapshot.completed = this.completed;
    return snapshot;
  }

  public Map<String, Vector3i> getBlockTypePosition() {
    return blockTypePosition;
  }

  public void setBlockTypePosition(Map<String, Vector3i> blockTypePosition) {
    this.blockTypePosition = blockTypePosition;
  }

  public Instant getCapturedAt() {
    return capturedAt;
  }

  public void setCapturedAt(Instant capturedAt) {
    this.capturedAt = capturedAt;
  }

  public boolean isCompleted() {
    return completed;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

}
