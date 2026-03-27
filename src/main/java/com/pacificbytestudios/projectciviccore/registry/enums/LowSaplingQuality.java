package com.pacificbytestudios.projectciviccore.registry.enums;

import java.util.function.Supplier;

public enum LowSaplingQuality implements Supplier<String> {
  ASH("Plant_Sapling_Ash");

  private String id;

  private LowSaplingQuality(String id) {
    this.id = id;
  }

  @Override
  public String get() {
    return this.id;
  }
}
