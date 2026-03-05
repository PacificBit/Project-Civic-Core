package com.pacificbytestudios.projectciviccore.utils;

import java.nio.file.Path;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.FastRandom;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.buffer.PrefabBufferUtil;
import com.hypixel.hytale.server.core.prefab.selection.buffer.impl.IPrefabBuffer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.PrefabUtil;

public class PrefabSpawner {

  public static boolean spawnAssetPrefab(
      World world,
      Store<EntityStore> entityStore,
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
      PrefabUtil.paste(prefab, world, target, yaw, force, new FastRandom(), entityStore);
      return true;
    } finally {
      prefab.release();
    }
  }
}
