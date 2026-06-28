package com.pacificbytestudios.projectciviccore.registry.enums;

import java.util.function.Supplier;
import lombok.Getter;

public enum MidQualityMinerDrop implements Supplier<String> {
  THORIUM("Ore_Thorium", 35),
  COBALT("Ore_Cobalt", 35),
  COPPER("Ore_Copper", 15),
  IRON("Ore_Iron", 15);

  private String id;
  @Getter
  private Byte probability;

  private MidQualityMinerDrop(String id, int probability) {
    this.id = id;
    this.probability = (byte) probability;
  }

  @Override
  public String get() {
    return this.id;
  }
}
