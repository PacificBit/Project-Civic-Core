package com.pacificbytestudios.projectciviccore.food.enums;

import java.util.function.Supplier;

public enum TopFoodQuality implements Supplier<String> {
  CAESAR_SALAD("Village_Food_Salad_Caesar");

  private String id;

  private TopFoodQuality(String id) {
    this.id = id;
  }

  @Override
  public String get() {
    return this.id;
  }
}
