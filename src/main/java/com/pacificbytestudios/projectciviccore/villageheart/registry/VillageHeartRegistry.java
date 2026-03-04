package com.pacificbytestudios.projectciviccore.villageheart.registry;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public final class VillageHeartRegistry {
  private static VillageHeartRegistry INSTANCE;

  private final Map<ChunkLocation, Set<Ref<ChunkStore>>> villagesHeartMap = new ConcurrentHashMap<>();

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

  public void registerVillage(ChunkLocation location, Ref<ChunkStore> heartRef) {
    villagesHeartMap
        .computeIfAbsent(location, l -> ConcurrentHashMap.newKeySet())
        .add(heartRef);
  }

  public void unregisterVillage(ChunkLocation location, Ref<ChunkStore> heartRef) {

    villagesHeartMap.computeIfPresent(location, (loc, set) -> {
      set.remove(heartRef);
      return set.isEmpty() ? null : set;
    });

  }

  public Set<Ref<ChunkStore>> getVillagesHeartIn(ChunkLocation location) {
    Set<Ref<ChunkStore>> set = villagesHeartMap.get(location);

    if (set == null) {
      return Collections.emptySet();
    }

    return Collections.unmodifiableSet(set);
  }
}
