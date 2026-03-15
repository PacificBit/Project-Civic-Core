package com.pacificbytestudios.projectciviccore.village.enums;

public enum WorkplaceInteractibles {
  RELAX_SPOT("Relax_Spot"),
  CHAIR("Chair"),
  EATING_SPOT("Eating_Spot");

  private String key;

  private WorkplaceInteractibles(String key) {
    this.key = key;
  }

  public String getKey() {
    return this.key;
  }
}
