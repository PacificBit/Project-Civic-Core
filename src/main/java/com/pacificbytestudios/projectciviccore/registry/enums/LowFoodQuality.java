package com.pacificbytestudios.projectciviccore.registry.enums;

import java.util.function.Supplier;

public enum LowFoodQuality implements Supplier<String> {
  POPCORN("Village_Food_Popcorn"),
  BREAD("Village_Food_Bread");

  private String id;

  private LowFoodQuality(String id) {
    this.id = id;
  }

  @Override
  public String get() {
    return this.id;
  }
}
