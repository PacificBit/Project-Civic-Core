package com.pacificbytestudios.projectciviccore.codecs;

import com.pacificbytestudios.projectciviccore.village.components.workplace.RequestNewTaskComponent;
import com.pacificbytestudios.projectciviccore.village.core.workplacetask.BakeryWorkplaceTaskStrategy;
import com.pacificbytestudios.projectciviccore.village.core.workplacetask.FarmWorkplaceTaskStrategy;
import com.pacificbytestudios.projectciviccore.village.core.workplacetask.MineWorkplaceTaskStrategy;
import com.pacificbytestudios.projectciviccore.village.core.workplacetask.SawmillWorkplaceTaskStrategy;

public final class WorkplaceTaskStrategyCodecs {
  private static boolean registered = false;

  private WorkplaceTaskStrategyCodecs() {
  }

  public static void registerAll() {
    if (registered) {
      return;
    }
    registered = true;

    RequestNewTaskComponent.registerStrategyCodec(
        "FarmWorkplaceTaskStrategy",
        FarmWorkplaceTaskStrategy.class,
        FarmWorkplaceTaskStrategy.CODEC);

    RequestNewTaskComponent.registerStrategyCodec(
        "BakeryWorkplaceTaskStrategy",
        BakeryWorkplaceTaskStrategy.class,
        BakeryWorkplaceTaskStrategy.CODEC);

    RequestNewTaskComponent.registerStrategyCodec(
        "MineWorkplaceTaskStrategy",
        MineWorkplaceTaskStrategy.class,
        MineWorkplaceTaskStrategy.CODEC);

    RequestNewTaskComponent.registerStrategyCodec(
        "SawmillWorkplaceTaskStrategy",
        SawmillWorkplaceTaskStrategy.class,
        SawmillWorkplaceTaskStrategy.CODEC);
  }
}
