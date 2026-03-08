package com.pacificbytestudios.projectciviccore.codecs;

import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.RegisterWorkplaceTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnVillagerTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnWorkplaceTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.TryJobAssignmentTask;
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
        "TryJobAssignmentTask",
        TryJobAssignmentTask.class,
        TryJobAssignmentTask.CODEC);

    VillageTaskQueueComponent.registerTaskCodec(
        "UnregisterWorkplaceTask",
        UnregisterWorkplaceTask.class,
        UnregisterWorkplaceTask.CODEC);
  }
}
