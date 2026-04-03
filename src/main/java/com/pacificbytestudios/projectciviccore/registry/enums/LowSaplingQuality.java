package com.pacificbytestudios.projectciviccore.registry.enums;

import java.util.function.Supplier;

public enum LowSaplingQuality implements Supplier<String> {
  CEDAR("Plant_Sapling_Cedar");

  private String id;

  private LowSaplingQuality(String id) {
    this.id = id;
  }

  @Override
  public String get() {
    return this.id;
  }
}
