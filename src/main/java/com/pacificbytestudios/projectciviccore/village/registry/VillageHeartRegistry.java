package com.pacificbytestudios.projectciviccore.village.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.village.codec.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;

public final class VillageHeartRegistry {
  private static VillageHeartRegistry INSTANCE;

  private final Map<ChunkLocation, Ref<ChunkStore>> villagesHeartMap = new ConcurrentHashMap<>();
  private final Map<ClaimedAreaComponent, Ref<ChunkStore>> claimAreaToHeartRef = new ConcurrentHashMap<>();

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

  public void registerVillage(
      ChunkLocation location,
      Ref<ChunkStore> heartRef,
      VillageHeartComponent heartComponent) {
    // if (villagesHeartMap.containsKey(location) &&
    // villagesHeartMap.get(location).isValid()) {
    // return;
    // }

    claimAreaToHeartRef.put(heartComponent.getClaimedArea(), heartRef);
    villagesHeartMap.put(location, heartRef);
    return;
  }

  public void unregisterVillage(Ref<ChunkStore> heartRef) {
    for (Map.Entry<ChunkLocation, Ref<ChunkStore>> entry : villagesHeartMap.entrySet()) {
      if (entry.getValue().equals(heartRef)) {
        System.out.println("~~~~~Removing reference from villageHeart registry");
        villagesHeartMap.remove(entry.getKey());
      }
    }

    for (Map.Entry<ClaimedAreaComponent, Ref<ChunkStore>> entry : claimAreaToHeartRef.entrySet()) {
      if (entry.getValue().equals(heartRef)) {
        System.out.println("!!!!!Removing reference from claimArea registry");
        claimAreaToHeartRef.remove(entry.getKey());
      }
    }
  }

  public void updateClaimedArea(ClaimedAreaComponent oldArea, ClaimedAreaComponent newArea) {
    Ref<ChunkStore> heart = claimAreaToHeartRef.get(oldArea);
    if (heart == null) {
      return;
    }
    claimAreaToHeartRef.put(newArea, heart);
    claimAreaToHeartRef.remove(oldArea);
  }

  public Ref<ChunkStore> getVillageHeartInChunk(ChunkLocation location) {
    return villagesHeartMap.get(location);
  }

  public Ref<ChunkStore> getVillageHeartIn(Vector2i coord2d) {
    System.out.println(claimAreaToHeartRef.entrySet().size());
    for (Map.Entry<ClaimedAreaComponent, Ref<ChunkStore>> entry : claimAreaToHeartRef.entrySet()) {
      System.out.println(entry.getKey().toString());
      if (entry.getKey().isInside(coord2d)) {
        System.out.println("Found!!!");
        return entry.getValue();
      }
    }
    return null;
  }
}
