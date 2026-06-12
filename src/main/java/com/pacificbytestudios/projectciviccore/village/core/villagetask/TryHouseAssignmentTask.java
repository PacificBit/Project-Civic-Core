package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;

@SerializableClass
public class TryHouseAssignmentTask extends AVillageTask {
  public static final BuilderCodec<TryHouseAssignmentTask> CODEC = TryHouseAssignmentTaskCodec.CODEC;

  UUID house;

  public TryHouseAssignmentTask() {
  }

  public TryHouseAssignmentTask(UUID house) {
    this.house = house;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vec3i heartPos,
      VillageHeartComponent heart) {
    if (this.house == null) {
      this.house = heart.findHouseIdWithVacancy();
      if (this.house == null) {
        Logger.logInfo("No empty house found");
        return;
      }
    }

    HouseComponent houseComponent = heart.getHouseFromId(this.house);
    if (houseComponent == null) {
      Logger.logInfo("No house found for id: " + this.house);
      return;
    }

    ArrayDeque<UUID> homelessRegistry = new ArrayDeque<>(heart.getHomelessRegistry());
    if (homelessRegistry.size() == 0) {
      Logger.logInfo("No homeless villagers found");
      return;
    }

    int assigned = 0;
    Map<UUID, VillagerComponent> assignedVillagerSnapshots = new HashMap<>();
    while (houseComponent.hasVacancy() && homelessRegistry.size() > 0) {
      UUID candidate = homelessRegistry.pollFirst();
      if (candidate == null) {
        break;
      }

      if (heart.assignHouse(candidate, this.house)) {
        assigned++;
        VillagerComponent assignedVillagerSnapshot = VillageVillagerEntitySync.snapshotVillager(heart, candidate);
        if (assignedVillagerSnapshot != null) {
          assignedVillagerSnapshots.put(candidate, assignedVillagerSnapshot);
        }
      }
    }

    if (!assignedVillagerSnapshots.isEmpty()) {
      World world = chunk.getWorld();
      world.execute(() -> VillageVillagerEntitySync.persistVillagers(world, assignedVillagerSnapshots));
    }

    Logger.logInfo("Assigned " + assigned + " villagers to house: " + this.house);
  }
}
