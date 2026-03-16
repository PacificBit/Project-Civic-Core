package com.pacificbytestudios.projectciviccore.village.enums;

import java.util.function.Supplier;

public enum WorkplaceInteractibles implements Supplier<String> {
  RELAX_SPOT("Relax_Spot"),
  EATING_SPOT("Eating_Spot");

  private String key;

  private WorkplaceInteractibles(String key) {
    this.key = key;
  }

  @Override
  public String get() {
    return this.key;
  }
}
