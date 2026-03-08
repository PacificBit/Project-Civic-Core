package com.pacificbytestudios.projectciviccore.villagers.enums;

public enum WorkType {
  MINER("Miner"),
  FARMER("Farmer"),
  WOODCUTTER("Wood_Cutter"),
  BAKER("Baker");

  private final String roleId;

  WorkType(String roleId) {
    this.roleId = roleId;
  }

  public String getRoleId() {
    return roleId;
  }
}
