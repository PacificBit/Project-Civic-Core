package com.pacificbytestudios.projectciviccore.village.enums;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum VillageHeartState {
  NORMAL("Normal"), PANIC("Panic");

  private String state;
  private static String AVAILABLE_STATES;

  static {
    AVAILABLE_STATES = Stream.of(VillageHeartState.values()).map(VillageHeartState::getState)
        .collect(Collectors.joining(", "));
  }

  private VillageHeartState(String state) {
    this.state = state;
  }

  public String getState() {
    return state;
  }

  public static String getAvailableStates() {
    return AVAILABLE_STATES;
  }

}
