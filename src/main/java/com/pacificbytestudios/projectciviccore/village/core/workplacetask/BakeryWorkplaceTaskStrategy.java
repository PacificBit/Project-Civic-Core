package com.pacificbytestudios.projectciviccore.village.core.workplacetask;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudios.projectciviccore.village.components.workplace.WorkplaceComponent;

public class BakeryWorkplaceTaskStrategy implements IWorkplaceTaskStrategy {
  public static final BuilderCodec<BakeryWorkplaceTaskStrategy> CODEC = BuilderCodec
      .builder(BakeryWorkplaceTaskStrategy.class, BakeryWorkplaceTaskStrategy::new)

      .build();

  @Override
  public boolean execute(WorkplaceComponent workplace) {
    System.out.println("Hello from - BAKERY");
    return true;
  }

  public BakeryWorkplaceTaskStrategy clone() {
    BakeryWorkplaceTaskStrategy strategy = new BakeryWorkplaceTaskStrategy();
    return strategy;
  }
}
