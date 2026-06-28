package com.pacificbytestudios.projectciviccore.villagers.enums;

import lombok.Getter;

public enum VillagerRole {
  FEMALE("Female_Civilian", "Female"), MALE("Male_Civilian", "Male");

  @Getter
  private final String role;
  @Getter
  private final String gender;

  private VillagerRole(String role, String gender) {
    this.role = role;
    this.gender = gender;
  }

  public static VillagerRole fromNpcRole(String npcRole) {
    if (npcRole == null) {
      return null;
    }

    for (VillagerRole role : values()) {
      if (role.role.equals(npcRole)) {
        return role;
      }
    }

    return null;
  }
}
