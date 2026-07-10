package com.pacificbytestudios.projectciviccore.utils;

import java.nio.file.Path;

import org.bson.BsonDocument;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.Axis;
import com.hypixel.hytale.math.util.FastRandom;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.blocktype.component.BlockPhysics;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.prefab.PrefabLoadException;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.config.SelectionPrefabSerializer;
import com.hypixel.hytale.server.core.prefab.selection.buffer.PrefabBufferUtil;
import com.hypixel.hytale.server.core.prefab.selection.buffer.impl.IPrefabBuffer;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.PrefabUtil;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D.Vec2i;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;

public class PrefabManager {

  private PrefabManager() {
  }

  public static boolean spawnPrefab(
      World world,
      String prefabRelativePath,
      Vec3i target,
      Rotation yaw,
      boolean force) {
    return spawnTrackedPrefab(world, prefabRelativePath, target, yaw, force) != null;
  }

  public static TrackedPrefabPlacementComponent spawnTrackedPrefab(
      World world,
      String prefabRelativePath,
      Vec3i target,
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

  public static TrackedPrefabPlacementComponent captureTrackedPrefabSnapshot(
      World world,
      String footprintPrefabPath,
      Vec3i target,
      Rotation yaw) {
    if (world == null || target == null) {
      return null;
    }

    BlockSelection footprint = loadPrefabSelection(footprintPrefabPath);
    if (footprint == null) {
      return null;
    }

    BlockSelection transformedFootprint = footprint.cloneSelection();
    if (yaw != null && yaw != Rotation.None) {
      transformedFootprint = transformedFootprint.rotate(Axis.Y, yaw.getDegrees());
    }

    transformedFootprint.setPosition(target.x, target.y, target.z);

    BlockSelection restoreSelection = new BlockSelection(transformedFootprint.getBlockCount(), 0);
    restoreSelection.setAnchor(
        transformedFootprint.getAnchorX(),
        transformedFootprint.getAnchorY(),
        transformedFootprint.getAnchorZ());
    restoreSelection.setPosition(target.x, target.y, target.z);

    BlockSelection captureMask = transformedFootprint;
    captureMask.forEachBlock((localX, localY, localZ, block) -> {
      Vec3i worldPosition = getWorldPosition(captureMask, localX, localY, localZ);
      WorldChunk chunk = Utils.getWorldChunk(world, worldPosition);
      restoreSelection.copyFromAtWorld(
          worldPosition.x,
          worldPosition.y,
          worldPosition.z,
          chunk,
          getBlockPhysics(world, worldPosition));
    });

    BsonDocument restoreSnapshot = SelectionPrefabSerializer.serialize(restoreSelection);
    return new TrackedPrefabPlacementComponent(footprintPrefabPath, target, yaw, restoreSnapshot);
  }

  public static Vec2i getPrefabWidthDepth(String prefabRelativePath) {
    Vec3i dimensions = getPrefabWidthHeightDepth(prefabRelativePath);
    if (dimensions == null) {
      return null;
    }

    return new Vec2i(dimensions.x, dimensions.z);
  }

  public static Vec3i getPrefabWidthHeightDepth(String prefabRelativePath) {
    IPrefabBuffer prefabBuffer = loadPrefabBuffer(prefabRelativePath);
    if (prefabBuffer == null) {
      return null;
    }

    int width = prefabBuffer.getMaxX() - prefabBuffer.getMinX() + 1;
    int height = prefabBuffer.getMaxY() - prefabBuffer.getMinY() + 1;
    int depth = prefabBuffer.getMaxZ() - prefabBuffer.getMinZ() + 1;
    return new Vec3i(width, height, depth);
  }

  public static boolean restoreTrackedPrefab(
      World world,
      TrackedPrefabPlacementComponent trackedPlacement) {
    if (trackedPlacement == null || !trackedPlacement.hasRestoreSnapshot()) {
      return false;
    }

    BlockSelection restoreSelection = SelectionPrefabSerializer.deserialize(trackedPlacement.getRestoreSnapshot());
    Vec3i origin = trackedPlacement.getOrigin();
    restoreSelection.setPosition(origin.x, origin.y, origin.z);
    restoreSelection.place(ConsoleSender.INSTANCE, world);
    return true;
  }

  public static boolean removePrefab(
      World world,
      String prefabRelativePath,
      Vec3i target,
      Rotation yaw,
      boolean force) {
    Path prefabPath = PrefabStore.get().findAssetPrefabPath(prefabRelativePath);
    if (prefabPath == null) {
      return false;
    }

    IPrefabBuffer prefab = PrefabBufferUtil.getCached(prefabPath);
    PrefabUtil.remove(
        prefab,
        world,
        target,
        yaw,
        new FastRandom(),
        force ? PrefabUtil.Flags.FORCE : PrefabUtil.Flags.NONE,
        SetBlockSettings.NONE,
        1.0);
    return true;
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

  private static Vec3i getWorldPosition(
      BlockSelection selection,
      int localX,
      int localY,
      int localZ) {
    return new Vec3i(
        localX + selection.getX() - selection.getAnchorX(),
        localY + selection.getY() - selection.getAnchorY(),
        localZ + selection.getZ() - selection.getAnchorZ());
  }

  private static BlockPhysics getBlockPhysics(World world, Vec3i position) {
    Ref<ChunkStore> sectionRef = world.getChunkStore().getChunkSectionReferenceAtBlock(
        position.x,
        position.y,
        position.z);
    if (sectionRef == null || !sectionRef.isValid()) {
      return null;
    }

    return sectionRef.getStore().getComponent(sectionRef, BlockPhysics.getComponentType());
  }
}
