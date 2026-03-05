package com.pacificbytestudios.projectciviccore.village.enums;

public enum WorkplacePlaceType {
  MINE("mine"),
  SAWMILL("sawmill"),
  FARM("farm"),
  BAKERY("bakery");

  private String type;

  private WorkplacePlaceType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
