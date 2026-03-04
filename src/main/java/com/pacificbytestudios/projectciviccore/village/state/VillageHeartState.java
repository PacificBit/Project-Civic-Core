package com.pacificbytestudios.projectciviccore.village.state;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum VillageHeartState {
  NORMAL("normal"), PANIC("panic");

  private String state;
  private static Map<String, VillageHeartState> STATE_MAP = new HashMap<>();
  private static String AVAILABLE_STATES;

  static {
    for (VillageHeartState villageState : VillageHeartState.values()) {
      STATE_MAP.put(villageState.state, villageState);
    }

    AVAILABLE_STATES = Stream.of(VillageHeartState.values()).map(VillageHeartState::getState)
        .collect(Collectors.joining(", "));
  }

  private VillageHeartState(String state) {
    this.state = state;
  }

  public String getState() {
    return state;
  }

  public static VillageHeartState fromString(String strState) {
    return STATE_MAP.get(strState.toLowerCase());
  }

  public static String getAvailableStates() {
    return AVAILABLE_STATES;
  }

}
