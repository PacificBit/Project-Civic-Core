package com.pacificbytestudios.projectciviccore.village.core.workplacetask;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudios.projectciviccore.village.components.workplace.WorkplaceComponent;

public class SawmillWorkplaceTaskStrategy implements IWorkplaceTaskStrategy {
  public static final BuilderCodec<SawmillWorkplaceTaskStrategy> CODEC = BuilderCodec
      .builder(SawmillWorkplaceTaskStrategy.class, SawmillWorkplaceTaskStrategy::new)

      .build();

  @Override
  public boolean execute(WorkplaceComponent workplace) {
    System.out.println("Hello from - SAWMILL");
    return true;
  }

  public SawmillWorkplaceTaskStrategy clone() {
    SawmillWorkplaceTaskStrategy strategy = new SawmillWorkplaceTaskStrategy();
    return strategy;
  }
}
