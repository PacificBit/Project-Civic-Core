package com.pacificbytestudios.projectciviccore.village.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.pacificbytestudios.projectciviccore.village.codec.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.villagers.codecs.VillageHeartRef;

public final class VillageHeartRegistry {
  private static VillageHeartRegistry INSTANCE;

  private final Map<ClaimedAreaComponent, VillageHeartRef> claimAreaToHeartRef = new ConcurrentHashMap<>();

  public record ChunkLocation(int x, int z) {
  }

  public static VillageHeartRegistry get() {
    if (INSTANCE == null) {
      INSTANCE = new VillageHeartRegistry();
    }
    return INSTANCE;
  }

  public void registerVillage(
      String worldName,
      Vector3i position,
      ClaimedAreaComponent claimedArea) {
    claimAreaToHeartRef.put(claimedArea, new VillageHeartRef(worldName, position));
    return;
  }

  public void unregisterVillage(ClaimedAreaComponent claimedArea) {
    claimAreaToHeartRef.remove(claimedArea);
  }

  public void updateClaimedArea(ClaimedAreaComponent oldArea, ClaimedAreaComponent newArea) {
    VillageHeartRef heart = claimAreaToHeartRef.get(oldArea);
    if (heart == null) {
      return;
    }
    claimAreaToHeartRef.put(newArea, heart);
    claimAreaToHeartRef.remove(oldArea);
  }

  public VillageHeartRef getVillageHeartIn(Vector2i coord2d) {
    for (Map.Entry<ClaimedAreaComponent, VillageHeartRef> entry : claimAreaToHeartRef.entrySet()) {
      if (entry.getKey().isInside(coord2d)) {
        return entry.getValue();
      }
    }
    return null;
  }
}
