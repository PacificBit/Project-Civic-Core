package com.pacificbytestudios.projectciviccore.registry.enums;

import java.util.function.Supplier;

public enum MidSaplingQuality implements Supplier<String> {
  CEDAR("Plant_Sapling_Cedar");

  private String id;

  private MidSaplingQuality(String id) {
    this.id = id;
  }

  @Override
  public String get() {
    return this.id;
  }
}
