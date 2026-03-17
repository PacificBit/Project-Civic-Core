package com.pacificbytestudios.projectciviccore.recipe.enums;

import java.util.function.Supplier;

public enum LowToolQuality implements Supplier<String> {
  HOE("Village_Tool_Hoe_Crude"),
  HATCHET("Village_Tool_Hatchet_Crude"),
  PICKAXE("Village_Tool_Pickaxe_Crude"),
  WATERING_CAN("Village_Tool_Watering_Can");

  private String id;

  private LowToolQuality(String id) {
    this.id = id;
  }

  @Override
  public String get() {
    return this.id;
  }
}
