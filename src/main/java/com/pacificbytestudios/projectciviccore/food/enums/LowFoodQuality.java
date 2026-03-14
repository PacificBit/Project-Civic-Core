package com.pacificbytestudios.projectciviccore.food.enums;

import java.util.function.Supplier;

public enum LowFoodQuality implements Supplier<String> {
  POPCORN("Village_Food_Popcorn"),
  BREAD("Village_Food_Bread");

  private String id;

  private LowFoodQuality(String id) {
    this.id = id;
  }

  public String getId() {
    return this.id;
  }

  @Override
  public String get() {
    return this.id;
  }
}
