package com.pacificbytestudios.projectciviccore.registry.enums;

import java.util.function.Supplier;

public enum MidQualityMinerDrop implements Supplier<String> {
  THORIUM("Ore_Thorium", 35),
  COBALT("Ore_Cobalt", 35),
  COPPER("Ore_Copper", 15),
  IRON("Ore_Iron", 15);

  private String id;
  private Byte probability;

  private MidQualityMinerDrop(String id, int probability) {
    this.id = id;
    this.probability = (byte) probability;
  }

  @Override
  public String get() {
    return this.id;
  }

  public Byte getProbability() {
    return this.probability;
  }
}
