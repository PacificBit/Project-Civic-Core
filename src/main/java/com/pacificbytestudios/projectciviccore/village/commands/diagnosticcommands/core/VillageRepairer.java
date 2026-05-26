package com.pacificbytestudios.projectciviccore.village.commands.diagnosticcommands.core;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnVillagerTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.TryHouseAssignmentTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.TryJobAssignmentTask;

public class VillageRepairer {

  public static boolean repairVillage(World world, VillageHeartComponent heart) {
    boolean isMayorValid = isMayorValid(world, heart);
    Set<UUID> invalidVillagerRefCount = getInvalidVillagerRef(world, heart);
    return repairVillage(world, heart, new VillageStatus(isMayorValid, invalidVillagerRefCount));
  }

  public static boolean repairVillage(World world, VillageHeartComponent heart, VillageStatus status) {
    VillageTaskQueueComponent queueComponent = new VillageTaskQueueComponent();
    if (status.invalidVillagerRefCount.size() > 0) {
      for (UUID villagerId : status.invalidVillagerRefCount) {
        heart.removeVillagerLinks(villagerId);
        queueComponent.registerTask(new SpawnVillagerTask(
            heart.getNextRequiredRole().getRole(),
            3));
        queueComponent.registerTask(new TryJobAssignmentTask());
        queueComponent.registerTask(new TryHouseAssignmentTask());
      }
    }

    if (!status.isMayorValid) {
      queueComponent.registerTask(new TryJobAssignmentTask(heart.getTownHallId()));
    }

    world.execute(() -> {
      Utils.safeVillageTaskEnqueue(
          heart.getTownHall().getVillageHeartRef(),
          queueComponent);
    });
    return true;
  }

  public static boolean isMayorValid(World world, VillageHeartComponent heart) {
    try {
      return Utils.getMayorComponent(world, heart) != null;
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  public static int calculateInvalidVillageRefCount(World world, VillageHeartComponent heart) {
    return (int) getInvalidVillagerRef(world, heart).size();
  }

  public static Set<UUID> getInvalidVillagerRef(World world, VillageHeartComponent heart) {
    Predicate<UUID> predicate = id -> {
      Ref<EntityStore> ref = world.getEntityRef(id);
      return ref == null || !ref.isValid();
    };

    return heart.getVillagerIdToComponent()
        .keySet()
        .stream()
        .filter(predicate)
        .collect(Collectors.toSet());
  }

  public record VillageStatus(
      boolean isMayorValid,
      Set<UUID> invalidVillagerRefCount) {
  }
}
