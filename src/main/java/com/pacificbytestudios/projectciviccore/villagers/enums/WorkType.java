package com.pacificbytestudios.projectciviccore.villagers.enums;

import lombok.Getter;

public enum WorkType {
  MAYOR("Mayor"),
  MINER("Miner"),
  FARMER("Farmer"),
  WOOD_CUTTER("Wood_Cutter"),
  STORAGE_KEEPER("Storage_Keeper"),
  INN_KEEPER("Inn_Keeper");

  @Getter
  private final String roleId;

  WorkType(String roleId) {
    this.roleId = roleId;
  }
}
