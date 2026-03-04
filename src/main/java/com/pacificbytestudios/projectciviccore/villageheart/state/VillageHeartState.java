package com.pacificbytestudios.projectciviccore.villageheart.state;

import java.util.HashMap;
import java.util.Map;

public enum VillageHeartState {
  NORMAL("normal"), PANIC("panic");

  private String state;
  private static Map<String, VillageHeartState> STATE_MAP = new HashMap<>();

  static {
    for (VillageHeartState villageState : VillageHeartState.values()) {
      STATE_MAP.put(villageState.state, villageState);
    }
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

}
