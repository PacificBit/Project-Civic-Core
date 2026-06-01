package com.pacificbytestudios.projectciviccore.villagers.enums;

public enum WorkType {
  MAYOR("Mayor"),
  MINER("Miner"),
  FARMER("Farmer"),
  WOOD_CUTTER("Wood_Cutter"),
  SHOP_KEEPER("Shop_Keeper"),
  STORAGE_KEEPER("Storage_Keeper"),
  INN_KEEPER("Inn_Keeper");

  private final String roleId;

  WorkType(String roleId) {
    this.roleId = roleId;
  }

  public String getRoleId() {
    return roleId;
  }
}
