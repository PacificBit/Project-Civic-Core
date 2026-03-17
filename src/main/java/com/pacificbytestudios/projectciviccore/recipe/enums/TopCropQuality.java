package com.pacificbytestudios.projectciviccore.recipe.enums;

import java.util.function.Supplier;

import com.pacificbytestudios.projectciviccore.villagers.enums.Crops;

public enum TopCropQuality implements Supplier<String> {
  PUMPKIN(Crops.PUMPKIN),
  WHEAT(Crops.WHEAT),
  CORN(Crops.CORN),
  LETTUCE(Crops.LETTUCE),
  TOMATO(Crops.TOMATO);

  private Crops crop;

  private TopCropQuality(Crops crop) {
    this.crop = crop;
  }

  @Override
  public String get() {
    return this.crop.getSeedId();
  }
}
