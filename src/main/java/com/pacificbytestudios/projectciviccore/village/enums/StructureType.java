package com.pacificbytestudios.projectciviccore.village.enums;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum StructureType implements Supplier<String> {
  HOUSE("House"),
  TOWN_HALL("Town_Hall"),
  MINE("Mine"),
  SAWMILL("Sawmill"),
  FARM("Farm"),
  MARKET("Market"),
  STORAGE("Storage"),
  TAVERN("Tavern");

  private String type;

  private StructureType(String type) {
    this.type = type;
  }

  @Override
  public String get() {
    return this.type;
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
