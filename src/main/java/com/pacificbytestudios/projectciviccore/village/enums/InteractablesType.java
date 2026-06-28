package com.pacificbytestudios.projectciviccore.village.enums;

import java.util.function.Supplier;

import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;
import lombok.Getter;

public enum InteractablesType implements Supplier<String> {
  EATING_SPOT("Eating_Spot", MarkerType.TAVERN_CHAIR, MarkerType.TAVERN_PLATE);

  private String key;
  @Getter
  private MarkerType markerType;
  @Getter
  private MarkerType dependsOn;

  private InteractablesType(
      String key,
      MarkerType markerType,
      MarkerType dependsOn) {
    this.key = key;
    this.markerType = markerType;
    this.dependsOn = dependsOn;
  }

  @Override
  public String get() {
    return this.key;
  }
}
