package com.pacificbytestudios.projectciviccore.villagers.enums;

public enum WorkType {
  MINER("Miner"),
  FARMER("Farmer"),
  WOOD_CUTTER("Wood_Cutter"),
  STORAGE_KEEPER("Storage_Keeper"),
  BAKER("Baker");

  private final String roleId;

  WorkType(String roleId) {
    this.roleId = roleId;
  }

  public String getRoleId() {
    return roleId;
  }
}
