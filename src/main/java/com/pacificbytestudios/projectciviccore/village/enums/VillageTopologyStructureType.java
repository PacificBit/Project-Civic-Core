package com.pacificbytestudios.projectciviccore.village.enums;

import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig.Road.RoadType;

public enum VillageTopologyStructureType {
  CITY_CENTER(VillageTopologyStructureCategory.CORE),

  TOWN_HALL(VillageTopologyStructureCategory.PRIMARY_BUILDING),
  HOUSE(VillageTopologyStructureCategory.PRIMARY_BUILDING),
  STORAGE(VillageTopologyStructureCategory.PRIMARY_BUILDING),
  TAVERN(VillageTopologyStructureCategory.PRIMARY_BUILDING),
  MARKET(VillageTopologyStructureCategory.PRIMARY_BUILDING),

  MINE(VillageTopologyStructureCategory.SECONDARY_BUILDING),
  SAWMILL(VillageTopologyStructureCategory.SECONDARY_BUILDING),
  FARM(VillageTopologyStructureCategory.SECONDARY_BUILDING),

  MAIN_ROAD(VillageTopologyStructureCategory.STREET),
  SIDE_ROAD(VillageTopologyStructureCategory.STREET);

  private final VillageTopologyStructureCategory category;

  VillageTopologyStructureType(VillageTopologyStructureCategory category) {
    this.category = category;
  }

  public VillageTopologyStructureCategory getCategory() {
    return this.category;
  }

  public boolean isRoad() {
    return this.category == VillageTopologyStructureCategory.STREET;
  }

  public static VillageTopologyStructureType fromStructureType(StructureType structureType) {
    return switch (structureType) {
      case HOUSE -> HOUSE;
      case TOWN_HALL -> TOWN_HALL;
      case MINE -> MINE;
      case SAWMILL -> SAWMILL;
      case FARM -> FARM;
      case STORAGE -> STORAGE;
      case TAVERN -> TAVERN;
      case MARKET -> MARKET;
    };
  }

  public static VillageTopologyStructureType fromRoadType(RoadType roadType) {
    return switch (roadType) {
      case MAIN_ROAD -> MAIN_ROAD;
      case SIDE_ROAD -> SIDE_ROAD;
    };
  }
}
