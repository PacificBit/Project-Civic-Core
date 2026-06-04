package com.pacificbytestudios.projectciviccore.utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

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

  public static Vec3f rotateOffset(Vec3f offset, Rotation rotation) {
    if (offset == null) {
      return Vec3f.safeZero();
    }

    Rotation normalizedRotation = normalize(rotation);
    if (normalizedRotation == Rotation.None) {
      return Vec3f.from(offset);
    }

    return Vec3f.from(normalizedRotation.rotateY(offset, new Vec3f()));
  }

  public static Vec3i rotateOffset(Vec3i offset, Rotation rotation) {
    if (offset == null) {
      return Vec3i.safeZero();
    }

    Rotation normalizedRotation = normalize(rotation);
    if (normalizedRotation == Rotation.None) {
      return Vec3i.from(offset);
    }

    return Vec3i.from(normalizedRotation.rotateY(offset, new Vec3i()));
  }

  /**
   * Point-space origin plus point-space offset. Use when both values already represent precise world points.
   */
  public static Vec3f withOffset(Vec3f origin, Vec3f localOffset, Rotation rotation) {
    return origin.clone().add(rotateOffset(localOffset, rotation));
  }

  /**
   * Block-space origin plus point-space offset. Use for access/interactable targets derived from a block.
   */
  public static Vec3f withOffset(Vec3i origin, Vec3f localOffset, Rotation rotation) {
    return Vec3f.from(origin).add(rotateOffset(localOffset, rotation));
  }

  /**
   * Block-space origin plus block-space offset. Use for real block positions such as cores, signs, and containers.
   */
  public static Vec3i withOffset(Vec3i origin, Vec3i localOffset, Rotation rotation) {
    return origin.clone().add(rotateOffset(localOffset, rotation));
  }

  public static Vec3f removeOffset(Vec3f position, Vec3f localOffset, Rotation rotation) {
    Vec3f rotatedOffset = rotateOffset(localOffset, rotation);
    return position.clone().add(new Vec3f(-rotatedOffset.x, -rotatedOffset.y, -rotatedOffset.z));
  }

  public static Vec3i removeOffset(Vec3i position, Vec3i localOffset, Rotation rotation) {
    Vec3i rotatedOffset = rotateOffset(localOffset, rotation);
    return position.clone().add(new Vec3i(-rotatedOffset.x, -rotatedOffset.y, -rotatedOffset.z));
  }

  public static int toBlockRotationIndex(Rotation yaw) {
    return RotationTuple.of(normalize(yaw), Rotation.None, Rotation.None).index();
  }
}
