package com.pacificbytestudios.projectciviccore.utils;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class StructurePlacementUtils {

  private StructurePlacementUtils() {
  }

  public static Rotation getPlayerPlacementRotation(
      Store<EntityStore> entityStore,
      Ref<EntityStore> playerRef) {
    HeadRotation headRotation = entityStore.getComponent(playerRef, HeadRotation.getComponentType());
    if (headRotation != null) {
      return toCardinalRotation(headRotation.getRotation());
    }

    TransformComponent transform = entityStore.getComponent(playerRef, TransformComponent.getComponentType());
    if (transform != null) {
      return toCardinalRotation(transform.getRotation());
    }

    return Rotation.None;
  }

  public static Rotation toCardinalRotation(Rotation3f rotation) {
    if (rotation == null || Float.isNaN(rotation.yaw())) {
      return Rotation.None;
    }

    int quarterTurns = Math.floorMod((int) Math.round(Math.toDegrees(rotation.yaw()) / 90.0), 4);
    return switch (quarterTurns) {
      case 0 -> Rotation.None;
      case 1 -> Rotation.Ninety;
      case 2 -> Rotation.OneEighty;
      default -> Rotation.TwoSeventy;
    };
  }

  public static Rotation normalize(Rotation rotation) {
    return rotation == null ? Rotation.None : rotation;
  }

  public static Rotation addYaw(Rotation baseRotation, Rotation structureRotation) {
    return normalize(baseRotation).add(normalize(structureRotation));
  }

  public static Vector3i rotateOffset(Vector3ic offset, Rotation rotation) {
    if (offset == null) {
      return VectorUtil.clone(Vector3iUtil.ZERO);
    }

    Rotation normalizedRotation = normalize(rotation);
    if (normalizedRotation == Rotation.None) {
      return VectorUtil.clone(offset);
    }

    return normalizedRotation.rotateY(VectorUtil.clone(offset), new Vector3i());
  }

  public static Vector3i withOffset(Vector3i origin, Vector3i localOffset, Rotation rotation) {
    return VectorUtil.clone(origin).add(rotateOffset(localOffset, rotation));
  }

  public static Vector3i removeOffset(Vector3i position, Vector3i localOffset, Rotation rotation) {
    Vector3i rotatedOffset = rotateOffset(localOffset, rotation);
    return VectorUtil.clone(position).add(new Vector3i(-rotatedOffset.x, -rotatedOffset.y, -rotatedOffset.z));
  }

  public static int toBlockRotationIndex(Rotation yaw) {
    return RotationTuple.of(normalize(yaw), Rotation.None, Rotation.None).index();
  }
}
