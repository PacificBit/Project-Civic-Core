package com.pacificbytestudios.projectciviccore.village.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public final class VillageHeartRegistry {
  private static VillageHeartRegistry INSTANCE;

  private final Map<ChunkLocation, Ref<ChunkStore>> villagesHeartMap = new ConcurrentHashMap<>();

  public record ChunkLocation(int x, int z) {
  }

  public static VillageHeartRegistry get() {
    if (INSTANCE == null) {
      INSTANCE = new VillageHeartRegistry();
    }
    return INSTANCE;
  }

  private VillageHeartRegistry() {
  }

  // ====================================

  public boolean registerVillage(ChunkLocation location, Ref<ChunkStore> heartRef) {
    if (villagesHeartMap.containsKey(location) && villagesHeartMap.get(location).isValid()) {
      return false;
    }
    villagesHeartMap.put(location, heartRef);
    return true;
  }

  public void unregisterVillage(ChunkLocation location, Ref<ChunkStore> heartRef) {
    villagesHeartMap.remove(location, heartRef);
  }

  public Ref<ChunkStore> getVillageHeartIn(ChunkLocation location) {
    return villagesHeartMap.get(location);
  }

  public Ref<ChunkStore> getVillageHeartIn(Vector2i position) {
    return null;
  }
}
