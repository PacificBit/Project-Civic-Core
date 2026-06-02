package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.joml.Vector3i;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class TryHouseAssignmentTask extends AVillageTask {
  public static final BuilderCodec<TryHouseAssignmentTask> CODEC = BuilderCodec
      .builder(TryHouseAssignmentTask.class, TryHouseAssignmentTask::new)

      .append(new KeyedCodec<>("House", Codec.UUID_BINARY),
          (obj, value) -> obj.house = value,
          obj -> obj.house)
      .add()

      .build();

  private UUID house;

  public TryHouseAssignmentTask() {
  }

  public TryHouseAssignmentTask(UUID house) {
    this.house = house;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vector3i heartPos,
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
