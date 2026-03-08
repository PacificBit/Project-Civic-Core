package com.pacificbytestudios.projectciviccore.village.core.workplacetask;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.pacificbytestudios.projectciviccore.village.components.workplace.WorkplaceComponent;

public class FarmWorkplaceTaskStrategy implements IWorkplaceTaskStrategy {
  public static final BuilderCodec<FarmWorkplaceTaskStrategy> CODEC = BuilderCodec
      .builder(FarmWorkplaceTaskStrategy.class, FarmWorkplaceTaskStrategy::new)

      .append(new KeyedCodec<>("IsFirstRun", Codec.BOOLEAN),
          (obj, value) -> obj.isFirstRun = (value == null) ? true : value,
          (obj) -> obj.isFirstRun)
      .add()

      .build();

  private Boolean isFirstRun = true;

  @Override
  public boolean execute(WorkplaceComponent workplace) {
    System.out.println("FARM is first run? " + isFirstRun);
    if (isFirstRun) {
      isFirstRun = false;
      // INFO: Check if the workplace component has a new plan
    }
    return false;
  }

  @Override
  public FarmWorkplaceTaskStrategy clone() {
    FarmWorkplaceTaskStrategy strategy = new FarmWorkplaceTaskStrategy();
    strategy.isFirstRun = isFirstRun;
    return strategy;
  }

  @Override
  public String toString() {
    return "FarmWorkplaceTaskStrategy {isFirstRun=" + isFirstRun + "}";
  }

}
