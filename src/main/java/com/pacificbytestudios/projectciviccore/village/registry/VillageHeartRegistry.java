package com.pacificbytestudios.projectciviccore.village.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.joml.Vector2i;
import org.joml.Vector3i;

import com.pacificbytestudios.projectciviccore.codecs.AreaFootprint;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public final class VillageHeartRegistry {
  private static VillageHeartRegistry INSTANCE;

  private final Map<UUID, AreaFootprint> villageIdToArea = new HashMap<>();
  private final Map<AreaFootprint, EntityRef> claimAreaToHeartRef = new HashMap<>();

  public record ChunkLocation(int x, int z) {
  }

  public static VillageHeartRegistry get() {
    if (INSTANCE == null) {
      INSTANCE = new VillageHeartRegistry();
    }
    return INSTANCE;
  }

  public void registerVillage(
      UUID uuid,
      String worldName,
      Vector3i position,
      AreaFootprint claimedArea) {
    villageIdToArea.put(uuid, claimedArea);
    claimAreaToHeartRef.put(claimedArea, new EntityRef(worldName, position));

    Logger.logInfo("Successfully registered village with id: " + uuid.toString());
  }

  public void unregisterVillage(UUID villageUUID) {
    if (!villageIdToArea.containsKey(villageUUID)) {
      Logger.logInfo("No village registered with id: " + villageUUID.toString());
      return;
    }
    AreaFootprint area = villageIdToArea.get(villageUUID);

    villageIdToArea.remove(villageUUID);
    if (area == null) {
      Logger.logInfo("No village claimed area registered with id: " + villageUUID.toString());
      return;
    }

    claimAreaToHeartRef.remove(area);
    Logger.logInfo("Village removed correctly");
  }

  public void updateClaimedArea(AreaFootprint oldArea, AreaFootprint newArea) {
    EntityRef heart = claimAreaToHeartRef.remove(oldArea);
    if (heart == null) {
      return;
    }

    claimAreaToHeartRef.put(newArea, heart);

    for (Map.Entry<UUID, AreaFootprint> entry : villageIdToArea.entrySet()) {
      if (entry.getValue().equals(oldArea)) {
        entry.setValue(newArea);
        break;
      }
    }
  }

  public EntityRef getVillageHeartIn(Vector2i coord2d) {
    for (Map.Entry<AreaFootprint, EntityRef> entry : claimAreaToHeartRef.entrySet()) {
      if (entry.getKey().isInside(coord2d)) {
        return entry.getValue();
      }
    }
    return null;
  }
}
