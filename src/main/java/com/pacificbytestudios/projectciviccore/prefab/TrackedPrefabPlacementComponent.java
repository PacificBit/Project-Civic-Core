package com.pacificbytestudios.projectciviccore.prefab;

import org.bson.BsonDocument;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerialWithCodec;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import lombok.Getter;

@Getter
@SerializableClass
public class TrackedPrefabPlacementComponent implements Component<ChunkStore> {
  public static final BuilderCodec<TrackedPrefabPlacementComponent> CODEC = TrackedPrefabPlacementComponentCodec.CODEC;

  String prefabPath;
  Vec3i origin = Vec3i.ZERO;
  Rotation yaw = Rotation.None;

  @SerialWithCodec("com.pacificbytestudios.projectciviccore.codecs.BsonDocumentValueCodec.INSTANCE")
  BsonDocument restoreSnapshot = new BsonDocument();

  public TrackedPrefabPlacementComponent() {
  }

  public TrackedPrefabPlacementComponent(
      String prefabPath,
      Vec3i origin,
      Rotation yaw,
      BsonDocument restoreSnapshot) {
    this.prefabPath = prefabPath;
    this.origin = origin == null ? Vec3i.safeZero() : Vec3i.from(origin);
    this.yaw = yaw == null ? Rotation.None : yaw;
    this.restoreSnapshot = restoreSnapshot == null ? new BsonDocument() : restoreSnapshot.clone();
  }

  public Vec3i getOrigin() {
    return origin == null ? Vec3i.safeZero() : Vec3i.from(origin);
  }

  public BsonDocument getRestoreSnapshot() {
    return restoreSnapshot == null ? new BsonDocument() : restoreSnapshot.clone();
  }

  public boolean hasRestoreSnapshot() {
    return restoreSnapshot != null && !restoreSnapshot.isEmpty();
  }

  @Override
  public TrackedPrefabPlacementComponent clone() {
    return TrackedPrefabPlacementComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, TrackedPrefabPlacementComponent> getComponentType() {
    return ProjectCivicCore.get().getTrackedPrefabPlacementComponentType();
  }
}
