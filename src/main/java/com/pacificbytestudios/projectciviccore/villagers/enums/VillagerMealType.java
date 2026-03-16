package com.pacificbytestudios.projectciviccore.villagers.enums;

import java.util.function.Supplier;

public enum VillagerMealType implements Supplier<String> {
  LUNCH("Lunch"),
  DINNER("Dinner");

  private final String displayName;

  VillagerMealType(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String get() {
    return this.displayName;
  }
}
