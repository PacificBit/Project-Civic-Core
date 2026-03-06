package com.pacificbytestudios.projectciviccore.village.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.codecs.VillageHeartRef;

public final class VillageHeartRegistry {
  private static VillageHeartRegistry INSTANCE;

  private final Map<UUID, ClaimedAreaComponent> villageIdToArea = new HashMap<>();
  private final Map<ClaimedAreaComponent, VillageHeartRef> claimAreaToHeartRef = new HashMap<>();

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
      ClaimedAreaComponent claimedArea) {
    villageIdToArea.put(uuid, claimedArea);
    claimAreaToHeartRef.put(claimedArea, new VillageHeartRef(worldName, position));

    ProjectCivicCore.LOGGER.atInfo()
        .log("[registerVillage] Successfully registered village with id: " + uuid.toString());
  }

  public void unregisterVillage(UUID villageUUID) {
    if (!villageIdToArea.containsKey(villageUUID)) {
      ProjectCivicCore.LOGGER.atInfo()
          .log("[unregisterVillage] No village registered with id: " + villageUUID.toString());
      return;
    }
    ClaimedAreaComponent area = villageIdToArea.get(villageUUID);

    villageIdToArea.remove(villageUUID);
    if (area == null) {
      ProjectCivicCore.LOGGER.atInfo()
          .log("[unregisterVillage] No village claimed area registered with id: " + villageUUID.toString());
      return;
    }

    claimAreaToHeartRef.remove(area);
    ProjectCivicCore.LOGGER.atInfo()
        .log("[unregisterVillage] Village removed correctly");
  }

  public void updateClaimedArea(ClaimedAreaComponent oldArea, ClaimedAreaComponent newArea) {
    VillageHeartRef heart = claimAreaToHeartRef.remove(oldArea);
    if (heart == null) {
      return;
    }

    claimAreaToHeartRef.put(newArea, heart);

    for (Map.Entry<UUID, ClaimedAreaComponent> entry : villageIdToArea.entrySet()) {
      if (entry.getValue().equals(oldArea)) {
        entry.setValue(newArea);
        break;
      }
    }
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
