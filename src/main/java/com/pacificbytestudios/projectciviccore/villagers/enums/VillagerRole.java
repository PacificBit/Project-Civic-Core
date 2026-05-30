package com.pacificbytestudios.projectciviccore.villagers.enums;

public enum VillagerRole {
  FEMALE("Female_Civilian", "Female"), MALE("Male_Civilian", "Male");

  private final String role;
  private final String gender;

  private VillagerRole(String role, String gender) {
    this.role = role;
    this.gender = gender;
  }

  public String getRole() {
    return this.role;
  }

  public String getGender() {
    return gender;
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
