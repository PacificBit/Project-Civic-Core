package com.pacificbytestudios.projectciviccore.village.enums;

import java.util.function.Supplier;

public enum StructureType implements Supplier<String> {
  HOUSE("House"),
  TOWN_HALL("Town_Hall"),
  MINE("Mine"),
  SAWMILL("Sawmill"),
  FARM("Farm"),
  STORAGE("Storage"),
  TAVERN("Tavern");

  private String type;

  private StructureType(String type) {
    this.type = type;
  }

  @Override
  public String get() {
    return this.type;
  }
}
