package com.pacificbytestudios.projectciviccore.codecs;

import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.ContinueBuildingTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.EnqueueVillageStorageRequestTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.ForceFillWorkplaceVacancyTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.LevelUpVillageTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.RegisterHouseTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.RegisterStructureTopologyTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.RegisterWorkplaceTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnHouseTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnVillagerTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnWorkplaceTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.TeleportVillagerToTownHeartTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.TryHouseAssignmentTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.TryJobAssignmentTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.UnregisterHouseTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.UnregisterWorkplaceTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.UpgradeBuildingTask;

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
        "RegisterStructureTopologyTask",
        RegisterStructureTopologyTask.class,
        RegisterStructureTopologyTask.CODEC);

    VillageTaskQueueComponent.registerTaskCodec(
        "ContinueBuildingTask",
        ContinueBuildingTask.class,
        ContinueBuildingTask.CODEC);

    VillageTaskQueueComponent.registerTaskCodec(
        "UpgradeBuildingTask",
        UpgradeBuildingTask.class,
        UpgradeBuildingTask.CODEC);

    VillageTaskQueueComponent.registerTaskCodec(
        "TeleportVillagerToTownHeartTask",
        TeleportVillagerToTownHeartTask.class,
        TeleportVillagerToTownHeartTask.CODEC);

    VillageTaskQueueComponent.registerTaskCodec(
        "SpawnVillagerTask",
        SpawnVillagerTask.class,
        SpawnVillagerTask.CODEC);

    VillageTaskQueueComponent.registerTaskCodec(
        "SpawnWorkplaceTask",
        SpawnWorkplaceTask.class,
        SpawnWorkplaceTask.CODEC);

    VillageTaskQueueComponent.registerTaskCodec(
        "SpawnHouseTask",
        SpawnHouseTask.class,
        SpawnHouseTask.CODEC);

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

    VillageTaskQueueComponent.registerTaskCodec(
        "LevelUpVillageTask",
        LevelUpVillageTask.class,
        LevelUpVillageTask.CODEC);

    VillageTaskQueueComponent.registerTaskCodec(
        "ForceFillWorkplaceVacancyTask",
        ForceFillWorkplaceVacancyTask.class,
        ForceFillWorkplaceVacancyTask.CODEC);

  }
}
