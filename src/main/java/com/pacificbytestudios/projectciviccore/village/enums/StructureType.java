package com.pacificbytestudios.projectciviccore.village.enums;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.pacificbytestudios.projectciviccore.village.commands.core.utils.MarkerType;

public enum StructureType implements Supplier<String> {
  TOWN_HALL("Town_Hall", MarkerType.DEFAULTS),
  MINE("Mine", MarkerType.DEFAULTS),
  SAWMILL("Sawmill", MarkerType.DEFAULTS),
  FARM("Farm", MarkerType.DEFAULTS),
  STORAGE("Storage", MarkerType.DEFAULTS),
  HOUSE("House", MarkerType.DEFAULTS),
  TAVERN("Tavern",
      MarkerType.markersTypeBuilder(
          MarkerType.DEFAULTS,
          MarkerType.TAVERN_CHAIR,
          MarkerType.TAVERN_PLATE));

  private String type;
  private MarkerType[] markerTypes;

  private StructureType(String type, MarkerType[] markerTypes) {
    this.type = type;
    this.markerTypes = markerTypes;
  }

  @Override
  public String get() {
    return this.type;
  }

  public MarkerType[] getMarkerTypes() {
    return markerTypes;
  }

  public static String getTypes() {
    return Stream.of(StructureType.values())
        .map(StructureType::get)
        .map(String::toLowerCase)
        .collect(Collectors.joining(", "));
  }

  public static Optional<StructureType> from(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }

    String normalizedValue = value.trim().toUpperCase(Locale.ROOT);

    try {
      return Optional.of(StructureType.valueOf(normalizedValue));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }
}
