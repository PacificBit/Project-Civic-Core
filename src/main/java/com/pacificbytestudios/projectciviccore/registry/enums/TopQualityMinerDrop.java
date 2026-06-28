package com.pacificbytestudios.projectciviccore.registry.enums;

import java.util.function.Supplier;
import lombok.Getter;

public enum TopQualityMinerDrop implements Supplier<String> {
  ADAMANTITE("Ore_Adamantite", 20),
  GOLD("Ore_Gold", 20),
  SILVER("Ore_Silver", 20),
  THORIUM("Ore_Thorium", 10),
  COBALT("Ore_Cobalt", 10),
  COPPER("Ore_Copper", 10),
  IRON("Ore_Iron", 10);

  private String id;
  @Getter
  private Byte probability;

  private TopQualityMinerDrop(String id, int probability) {
    this.id = id;
    this.probability = (byte) probability;
  }

  @Override
  public String get() {
    return this.id;
  }
}
