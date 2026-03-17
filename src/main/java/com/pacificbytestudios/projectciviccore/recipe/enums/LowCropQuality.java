package com.pacificbytestudios.projectciviccore.recipe.enums;

import java.util.function.Supplier;

import com.pacificbytestudios.projectciviccore.villagers.enums.Crops;

public enum LowCropQuality implements Supplier<String> {
  WHEAT(Crops.WHEAT),
  CORN(Crops.CORN);

  private Crops crop;

  private LowCropQuality(Crops crop) {
    this.crop = crop;
  }

  @Override
  public String get() {
    return this.crop.getSeedId();
  }
}
