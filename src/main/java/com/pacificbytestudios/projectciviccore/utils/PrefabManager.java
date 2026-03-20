package com.pacificbytestudios.projectciviccore.utils;

import java.nio.file.Path;

import org.bson.BsonDocument;

import com.hypixel.hytale.math.Axis;
import com.hypixel.hytale.math.util.FastRandom;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.prefab.PrefabLoadException;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.config.SelectionPrefabSerializer;
import com.hypixel.hytale.server.core.prefab.selection.buffer.PrefabBufferUtil;
import com.hypixel.hytale.server.core.prefab.selection.buffer.impl.IPrefabBuffer;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.PrefabUtil;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;

public class PrefabManager {

  private PrefabManager() {
  }

  public static boolean spawnPrefab(
      World world,
      String prefabRelativePath,
      Vector3i target,
      Rotation yaw,
      boolean force) {
    return spawnTrackedPrefab(world, prefabRelativePath, target, yaw, force) != null;
  }

  public static TrackedPrefabPlacementComponent spawnTrackedPrefab(
      World world,
      String prefabRelativePath,
      Vector3i target,
      Rotation yaw,
      boolean force) {
    BlockSelection prefab = loadPrefabSelection(prefabRelativePath);
    if (prefab == null) {
      return null;
    }

    BlockSelection selectionToPlace = prefab.cloneSelection();
    if (yaw != null && yaw != Rotation.None) {
      selectionToPlace = selectionToPlace.rotate(Axis.Y, yaw.getDegrees());
    }

    selectionToPlace.setPosition(target.x, target.y, target.z);
    BlockSelection before = selectionToPlace.place(ConsoleSender.INSTANCE, world);
    BsonDocument restoreSnapshot = SelectionPrefabSerializer.serialize(before);

    return new TrackedPrefabPlacementComponent(prefabRelativePath, target, yaw, restoreSnapshot);
  }

  public static Vector2i getPrefabWidthDepth(String prefabRelativePath) {
    Vector3i dimensions = getPrefabWidthHeightDepth(prefabRelativePath);
    if (dimensions == null) {
      return null;
    }

    return new Vector2i(dimensions.x, dimensions.z);
  }

  public static Vector3i getPrefabWidthHeightDepth(String prefabRelativePath) {
    IPrefabBuffer prefabBuffer = loadPrefabBuffer(prefabRelativePath);
    if (prefabBuffer == null) {
      return null;
    }

    try {
      int width = prefabBuffer.getMaxX() - prefabBuffer.getMinX() + 1;
      int height = prefabBuffer.getMaxY() - prefabBuffer.getMinY() + 1;
      int depth = prefabBuffer.getMaxZ() - prefabBuffer.getMinZ() + 1;
      return new Vector3i(width, height, depth);
    } finally {
      prefabBuffer.release();
    }
  }

  public static boolean restoreTrackedPrefab(
      World world,
      TrackedPrefabPlacementComponent trackedPlacement) {
    if (trackedPlacement == null || !trackedPlacement.hasRestoreSnapshot()) {
      return false;
    }

    BlockSelection restoreSelection = SelectionPrefabSerializer.deserialize(trackedPlacement.getRestoreSnapshot());
    Vector3i origin = trackedPlacement.getOrigin();
    restoreSelection.setPosition(origin.x, origin.y, origin.z);
    restoreSelection.place(ConsoleSender.INSTANCE, world);
    return true;
  }

  public static boolean removePrefab(
      World world,
      String prefabRelativePath,
      Vector3i target,
      Rotation yaw,
      boolean force) {
    Path prefabPath = PrefabStore.get().findAssetPrefabPath(prefabRelativePath);
    if (prefabPath == null) {
      return false;
    }

    IPrefabBuffer prefab = PrefabBufferUtil.getCached(prefabPath);
    try {
      PrefabUtil.remove(prefab, world, target, yaw, force, new FastRandom(), 0, 1.0);
      return true;
    } finally {
      prefab.release();
    }
  }

  private static BlockSelection loadPrefabSelection(String prefabRelativePath) {
    Path prefabPath = PrefabStore.get().findAssetPrefabPath(prefabRelativePath);
    if (prefabPath == null) {
      return null;
    }

    try {
      return PrefabStore.get().getPrefab(prefabPath);
    } catch (PrefabLoadException e) {
      return null;
    }
  }

  private static IPrefabBuffer loadPrefabBuffer(String prefabRelativePath) {
    Path prefabPath = PrefabStore.get().findAssetPrefabPath(prefabRelativePath);
    if (prefabPath == null) {
      return null;
    }

    return PrefabBufferUtil.getCached(prefabPath);
  }
}
