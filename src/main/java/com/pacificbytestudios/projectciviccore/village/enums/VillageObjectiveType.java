package com.pacificbytestudios.projectciviccore.village.enums;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum VillageObjectiveType {
  ASSIGN_HOUSE((short) 1),
  ASSIGN_WORKER((short) 1),
  VILLAGE_LEVEL_UP((short) 1),
  SPAWN_VILLAGER((short) 1),
  BUILD_HOUSE((short) 3),
  UPGRADE_BUILDING((short) -1),
  BUILD_WORKPLACE((short) 3),
  BUILD_DEFENCE((short) 2),
  // NOTE: take the demolish stages from the type you are destroying
  DEMOLISH((short) -1);

  private short stages;
  private static final Map<StructureType, VillageObjectiveType> STRUCTURE_TYPE_TO_OBJECTIVE;

  static {
    Map<StructureType, VillageObjectiveType> structureMap = new ConcurrentHashMap<>();
    structureMap.put(StructureType.HOUSE, BUILD_HOUSE);

    structureMap.put(StructureType.FARM, BUILD_WORKPLACE);
    structureMap.put(StructureType.STORAGE, BUILD_WORKPLACE);
    structureMap.put(StructureType.TAVERN, BUILD_WORKPLACE);
    structureMap.put(StructureType.SAWMILL, BUILD_WORKPLACE);
    structureMap.put(StructureType.MINE, BUILD_WORKPLACE);
    structureMap.put(StructureType.TOWN_HALL, BUILD_WORKPLACE);
    STRUCTURE_TYPE_TO_OBJECTIVE = structureMap;
  }

  private VillageObjectiveType(short stages) {
    this.stages = stages;
  }

  public short getStages() {
    return this.stages;
  }

  public static short getStages(StructureType structureType) {
    return STRUCTURE_TYPE_TO_OBJECTIVE.get(structureType).getStages();
  }
}
