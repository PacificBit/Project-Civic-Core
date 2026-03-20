package com.pacificbytestudios.projectciviccore.villagers.enums;

public enum VillagerRole {
  FEMALE("Female_Civilian"), MALE("Male_Civilian");

  private final String role;

  private VillagerRole(String role) {
    this.role = role;
  }

  public String getRole() {
    return this.role;
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
