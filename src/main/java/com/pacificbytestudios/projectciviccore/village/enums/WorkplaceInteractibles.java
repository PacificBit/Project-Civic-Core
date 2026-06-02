package com.pacificbytestudios.projectciviccore.village.enums;

import java.util.function.Supplier;

import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;

public enum WorkplaceInteractibles implements Supplier<String> {
  EATING_SPOT("Eating_Spot", MarkerType.TAVERN_CHAIR, MarkerType.TAVERN_PLATE);

  private String key;
  private MarkerType markerType;
  private MarkerType dependsOn;

  private WorkplaceInteractibles(
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

  public MarkerType getMarkerType() {
    return markerType;
  }

  public MarkerType getDependsOn() {
    return dependsOn;
  }
}
