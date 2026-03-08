package com.pacificbytestudios.projectciviccore.village.core.workplacetask;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudios.projectciviccore.village.components.workplace.WorkplaceComponent;

public class MineWorkplaceTaskStrategy implements IWorkplaceTaskStrategy {
  public static final BuilderCodec<MineWorkplaceTaskStrategy> CODEC = BuilderCodec
      .builder(MineWorkplaceTaskStrategy.class, MineWorkplaceTaskStrategy::new)

      .build();

  @Override
  public boolean execute(WorkplaceComponent workplace) {
    System.out.println("Hello from - MINE");
    return true;
  }

  public MineWorkplaceTaskStrategy clone() {
    MineWorkplaceTaskStrategy strategy = new MineWorkplaceTaskStrategy();
    return strategy;
  }
}
