package com.pacificbytestudios.projectciviccore.prefab;

import org.bson.BsonDocument;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public class TrackedPrefabPlacementComponent implements Component<ChunkStore> {

  public static final BuilderCodec<TrackedPrefabPlacementComponent> CODEC = BuilderCodec
      .builder(TrackedPrefabPlacementComponent.class, TrackedPrefabPlacementComponent::new)

      .append(new KeyedCodec<>("PrefabPath", Codec.STRING),
          (obj, value) -> obj.prefabPath = value,
          TrackedPrefabPlacementComponent::getPrefabPath)
      .add()

      .append(new KeyedCodec<>("Origin", Vector3i.CODEC),
          (obj, value) -> obj.origin = value,
          obj -> obj.origin)
      .add()

      .append(new KeyedCodec<>("Yaw", Rotation.CODEC),
          (obj, value) -> obj.yaw = value,
          TrackedPrefabPlacementComponent::getYaw)
      .add()

      .append(new KeyedCodec<>("RestoreSnapshot", Codec.BSON_DOCUMENT),
          (obj, value) -> obj.restoreSnapshot = value,
          obj -> obj.restoreSnapshot)
      .add()

      .build();

  private String prefabPath;
  private Vector3i origin = Vector3i.ZERO;
  private Rotation yaw = Rotation.None;
  private BsonDocument restoreSnapshot = new BsonDocument();

  public TrackedPrefabPlacementComponent() {
  }

  public TrackedPrefabPlacementComponent(
      String prefabPath,
      Vector3i origin,
      Rotation yaw,
      BsonDocument restoreSnapshot) {
    this.prefabPath = prefabPath;
    this.origin = origin == null ? Vector3i.ZERO : origin.clone();
    this.yaw = yaw == null ? Rotation.None : yaw;
    this.restoreSnapshot = restoreSnapshot == null ? new BsonDocument() : restoreSnapshot.clone();
  }

  public String getPrefabPath() {
    return prefabPath;
  }

  public Vector3i getOrigin() {
    return origin == null ? Vector3i.ZERO : origin.clone();
  }

  public Rotation getYaw() {
    return yaw;
  }

  public BsonDocument getRestoreSnapshot() {
    return restoreSnapshot == null ? new BsonDocument() : restoreSnapshot.clone();
  }

  public boolean hasRestoreSnapshot() {
    return restoreSnapshot != null && !restoreSnapshot.isEmpty();
  }

  @Override
  public TrackedPrefabPlacementComponent clone() {
    return new TrackedPrefabPlacementComponent(this.prefabPath, this.origin, this.yaw, this.restoreSnapshot);
  }

  public static ComponentType<ChunkStore, TrackedPrefabPlacementComponent> getComponentType() {
    return ProjectCivicCore.get().getTrackedPrefabPlacementComponentType();
  }
}
