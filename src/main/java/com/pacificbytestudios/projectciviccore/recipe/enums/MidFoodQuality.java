package com.pacificbytestudios.projectciviccore.recipe.enums;

import java.util.function.Supplier;

public enum MidFoodQuality implements Supplier<String> {
  PUMPKIN_PIE("Village_Food_Pie_Pumpkin"),
  BERRY_SALAD("Village_Food_Salad_Berry");

  private String id;

  private MidFoodQuality(String id) {
    this.id = id;
  }

  @Override
  public String get() {
    return this.id;
  }
}
