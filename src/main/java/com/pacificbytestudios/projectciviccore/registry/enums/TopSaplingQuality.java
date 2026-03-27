package com.pacificbytestudios.projectciviccore.registry.enums;

import java.util.function.Supplier;

public enum TopSaplingQuality implements Supplier<String> {
  SPRUCE("Plant_Sapling_Spruce");

  private String id;

  private TopSaplingQuality(String id) {
    this.id = id;
  }

  @Override
  public String get() {
    return this.id;
  }
}
