package com.pacificbytestudios.projectciviccore.codecs;

import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.EnqueueVillageStorageRequestTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.RegisterHouseTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.RegisterWorkplaceTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnVillagerTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnWorkplaceTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.TryHouseAssignmentTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.TryJobAssignmentTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.UnregisterHouseTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.UnregisterWorkplaceTask;

public final class VillageTaskQueueCodecs {
  private static boolean registered = false;

  private VillageTaskQueueCodecs() {
  }

  public static void registerAll() {
    if (registered) {
      return;
    }
    registered = true;

    VillageTaskQueueComponent.registerTaskCodec(
        "RegisterHouseTask",
        RegisterHouseTask.class,
        RegisterHouseTask.CODEC);

    VillageTaskQueueComponent.registerTaskCodec(
        "RegisterWorkplaceTask",
        RegisterWorkplaceTask.class,
        RegisterWorkplaceTask.CODEC);

    VillageTaskQueueComponent.registerTaskCodec(
        "SpawnVillagerTask",
        SpawnVillagerTask.class,
        SpawnVillagerTask.CODEC);

    VillageTaskQueueComponent.registerTaskCodec(
        "SpawnWorkplaceTask",
        SpawnWorkplaceTask.class,
        SpawnWorkplaceTask.CODEC);

    VillageTaskQueueComponent.registerTaskCodec(
        "TryHouseAssignmentTask",
        TryHouseAssignmentTask.class,
        TryHouseAssignmentTask.CODEC);

    VillageTaskQueueComponent.registerTaskCodec(
        "TryJobAssignmentTask",
        TryJobAssignmentTask.class,
        TryJobAssignmentTask.CODEC);

    VillageTaskQueueComponent.registerTaskCodec(
        "UnregisterHouseTask",
        UnregisterHouseTask.class,
        UnregisterHouseTask.CODEC);

    VillageTaskQueueComponent.registerTaskCodec(
        "UnregisterWorkplaceTask",
        UnregisterWorkplaceTask.class,
        UnregisterWorkplaceTask.CODEC);

    VillageTaskQueueComponent.registerTaskCodec(
        "RegisterNewVillageStorageRequest",
        EnqueueVillageStorageRequestTask.class,
        EnqueueVillageStorageRequestTask.CODEC);
  }
}
