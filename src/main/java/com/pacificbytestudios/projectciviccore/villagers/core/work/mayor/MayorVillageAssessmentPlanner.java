package com.pacificbytestudios.projectciviccore.villagers.core.work.mayor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.registry.registry.VillageBuildingCostRegistry;
import com.pacificbytestudios.projectciviccore.registry.registry.VillageBuildingUpgradeCostRegistry;
import com.pacificbytestudios.projectciviccore.registry.registry.VillageLevelUpCostRegistry;
import com.pacificbytestudios.projectciviccore.registry.registry.VillageNewVillagerCostRegistry;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartConfigComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.TownHallComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan.VillageProject;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan.VillageProject.Weight;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageObjectiveStatus;
import com.pacificbytestudios.projectciviccore.village.enums.VillageObjectiveType;

public class MayorVillageAssessmentPlanner {

  private static final int MAX_NEW_BUILD = 2;
  private static final int MAX_UPGRADE_BUILD = 2;
  private static final StructureType[] WORKPLACE_BUILD_PRIORITY = new StructureType[] {
      StructureType.STORAGE, StructureType.FARM,
      StructureType.TAVERN, StructureType.SAWMILL, StructureType.MINE };
  private static final StructureType[] STRUCTURE_UPGRADE_PRIORITY = new StructureType[] {
      StructureType.STORAGE, StructureType.FARM, StructureType.TAVERN,
      StructureType.SAWMILL, StructureType.MINE };

  public record AssessmentResult(
      boolean success,
      VillageProjectPlan projectPlan,
      int projectsBefore,
      int projectsAfter) {
    public int addedProjects() {
      return Math.max(0, this.projectsAfter - this.projectsBefore);
    }
  }

  public AssessmentResult assess(
      TownHallComponent townHall,
      VillageHeartComponent village,
      VillageTopologyComponent topology,
      int dayOfYear) {
    if (townHall == null || village == null || topology == null) {
      return new AssessmentResult(false, null, 0, 0);
    }

    VillageProjectPlan projectPlan = townHall.getVillageProjectPlan();
    if (projectPlan == null) {
      projectPlan = new VillageProjectPlan();
      townHall.setVillageProjectPlan(projectPlan);
    }

    removeBlockedSpawnVillagerProjects(projectPlan, village);

    int projectsBefore = projectPlan.getProjectList().size();

    short villageLevel = (short) village.getLevel();
    VillageHeartConfigComponent config = village.getConfig();

    VillageLimits villageLimits = new VillageLimits(
        config.getVillagerCapPerLevel(villageLevel),
        config.getStructureLimitsPerLevel(villageLevel),
        config.getStructureMaxLevelPerLevel(villageLevel));

    Map<StructureType, Short> structureCount = village.getStructureTypeCount();

    townHall.setLastDayVillageAssessment(dayOfYear);
    populateTopPriorityProjects(projectPlan, village);

    if (!projectPlan.hasProjects() &&
        villageLevel < config.getMaxLevel() &&
        isStructureLimitReached(villageLimits, structureCount) &&
        areStructuresMaxedOut(villageLimits, village.getWorkplaces(), village.getHouses())) {
      VillageProject project = buildUpdateVillageProject((short) (villageLevel + 1));
      projectPlan.addProject(project);
      return buildResult(projectPlan, projectsBefore);
    }

    if (projectPlan.contains(VillageObjectiveType.VILLAGE_LEVEL_UP)) {
      return buildResult(projectPlan, projectsBefore);
    }

    populateHighPriorityProjects(projectPlan, villageLimits, village, topology);
    populateMidPriorityProjects(projectPlan, villageLimits, village, structureCount, topology);
    populateLowPriorityProjects(projectPlan, villageLimits, village);

    return buildResult(projectPlan, projectsBefore);
  }

  private AssessmentResult buildResult(
      VillageProjectPlan projectPlan,
      int projectsBefore) {
    int projectsAfter = projectPlan == null ? 0 : projectPlan.getProjectList().size();
    return new AssessmentResult(true, projectPlan, projectsBefore, projectsAfter);
  }

  private void populateHighPriorityProjects(
      VillageProjectPlan plan,
      VillageLimits limits,
      VillageHeartComponent village,
      VillageTopologyComponent topology) {

    if (requireNewHouses(plan, limits, village, topology)) {
      VillageProject project = buildNewHouseProject();
      plan.addProject(project);
      return;
    }

    if (village.findHouseWithVacancy() != null &&
        village.getVillagerCount() < limits.maxVillager &&
        !plan.contains(VillageObjectiveType.SPAWN_VILLAGER)) {
      VillageProject project = buildSpawnVillagerProject((short) village.getLevel());
      plan.addProject(project);
    }
  }

  private void populateTopPriorityProjects(
      VillageProjectPlan plan,
      VillageHeartComponent village) {
    if (village.getHomelessCount() > 0 &&
        village.findHouseWithVacancy() != null &&
        !plan.contains(VillageObjectiveType.ASSIGN_HOUSE)) {
      plan.addProject(buildAssignHouseProject());
    }

    if (!village.getUnemployedRegistry().isEmpty() &&
        village.getFirstWorkplaceWithMissingEmployee() != null &&
        !plan.contains(VillageObjectiveType.ASSIGN_WORKER)) {
      plan.addProject(buildAssignWorkerProject());
    }
  }

  private void populateMidPriorityProjects(
      VillageProjectPlan plan,
      VillageLimits limits,
      VillageHeartComponent village,
      Map<StructureType, Short> structureCount,
      VillageTopologyComponent topology) {

    short plannedNewWorkplace = plan.getObjectiveTypeCount(VillageObjectiveType.BUILD_WORKPLACE);
    if (plannedNewWorkplace < MAX_NEW_BUILD) {
      for (StructureType structureType : WORKPLACE_BUILD_PRIORITY) {
        if (hasStagedStructureOfType(village, topology, structureType)) {
          logSkippedStagedPlanning(structureType);
          continue;
        }

        if (structureCount.getOrDefault(structureType, (short) 0) < limits.structureMaxCount.get(structureType) &&
            !plan.contains(structureType)) {
          VillageProject project = buildNewWorkplaceProject(structureType);
          plan.addProject(project);
          plannedNewWorkplace += 1;
        }

        if (plannedNewWorkplace == MAX_NEW_BUILD) {
          return;
        }
      }
    }
  }

  private void populateLowPriorityProjects(
      VillageProjectPlan plan,
      VillageLimits limits,
      VillageHeartComponent village) {

    short plannedUpgrade = plan.getObjectiveTypeCount(VillageObjectiveType.UPGRADE_BUILDING);
    if (plannedUpgrade >= MAX_UPGRADE_BUILD) {
      return;
    }

    short maxLevel = limits.maxStructureLevel.get(StructureType.HOUSE);
    for (HouseComponent house : village.getHouses()) {
      if (house.getLevel() < maxLevel && !plan.contains(StructureType.HOUSE) && !plan.contains(house.getUuid())) {
        VillageProject project = buildNewUpgradeProject(
            StructureType.HOUSE,
            (short) (house.getLevel() + 1),
            house.getUuid());
        plan.addProject(project);
        plannedUpgrade += 1;
      }

      if (plannedUpgrade == MAX_UPGRADE_BUILD) {
        return;
      }
    }

    for (StructureType structureType : STRUCTURE_UPGRADE_PRIORITY) {
      List<WorkplaceComponent> workplaces = village.getWorkplaceOfType(structureType);
      maxLevel = limits.maxStructureLevel.get(structureType);
      for (WorkplaceComponent workplace : workplaces) {
        if (workplace.getLevel() < maxLevel && !plan.contains(structureType) && !plan.contains(workplace.getUuid())) {
          VillageProject project = buildNewUpgradeProject(
              structureType,
              (short) (workplace.getLevel() + 1),
              workplace.getUuid());
          plan.addProject(project);
          plannedUpgrade += 1;
        }

        if (plannedUpgrade == MAX_UPGRADE_BUILD) {
          return;
        }
      }
    }
  }

  private boolean isStructureLimitReached(VillageLimits limits, Map<StructureType, Short> currentState) {
    for (Map.Entry<StructureType, Short> limit : limits.structureMaxCount.entrySet()) {
      if (limit.getValue() > currentState.getOrDefault(limit.getKey(), (short) 0)) {
        return false;
      }
    }
    return true;
  }

  private boolean areStructuresMaxedOut(
      VillageLimits limits,
      List<WorkplaceComponent> workplaces,
      List<HouseComponent> houses) {
    Map<StructureType, Short> maxStructureLevel = limits.maxStructureLevel;
    for (WorkplaceComponent workplace : workplaces) {
      if (workplace.getLevel() < maxStructureLevel.get(workplace.getWorkplaceType())) {
        return false;
      }
    }

    Short houseMaxLevel = maxStructureLevel.get(StructureType.HOUSE);
    for (HouseComponent house : houses) {
      if (house.getLevel() < houseMaxLevel) {
        return false;
      }
    }
    return true;
  }

  private boolean requireNewHouses(
      VillageProjectPlan plan,
      VillageLimits limits,
      VillageHeartComponent village,
      VillageTopologyComponent topology) {
    if (hasStagedStructureOfType(village, topology, StructureType.HOUSE)) {
      logSkippedStagedPlanning(StructureType.HOUSE);
      return false;
    }

    return village.getHouseCount() < limits.structureMaxCount.get(StructureType.HOUSE) &&
        !plan.contains(VillageObjectiveType.BUILD_HOUSE) &&
        (village.getHomelessCount() != 0 || village.findHouseWithVacancy() == null);
  }

  private void removeBlockedSpawnVillagerProjects(
      VillageProjectPlan plan,
      VillageHeartComponent village) {
    if (plan == null || village == null || village.findHouseWithVacancy() != null) {
      return;
    }

    boolean removed = plan.getProjectList().removeIf(project -> project != null
        && project.getObjectiveType() == VillageObjectiveType.SPAWN_VILLAGER);
    if (removed) {
      ProjectCivicCore.LOGGER.atInfo()
          .log("[MakeVillageAssessment] Removed blocked villager spawn project because no house has a vacancy");
    }
  }

  private boolean hasStagedStructureOfType(
      VillageHeartComponent village,
      VillageTopologyComponent topology,
      StructureType structureType) {
    if (village == null || topology == null || structureType == null) {
      return false;
    }

    for (TopologyStructure structure : topology.getRegisteredTopologyStructures().values()) {
      if (structure == null ||
          structure.getStructureId() == null ||
          structure.getStructureType() != structureType) {
        continue;
      }

      if (!isStructureRegisteredInVillage(village, structureType, structure.getStructureId())) {
        return true;
      }
    }

    return false;
  }

  private boolean isStructureRegisteredInVillage(
      VillageHeartComponent village,
      StructureType structureType,
      UUID structureId) {
    if (structureType == StructureType.HOUSE) {
      return village.getHouseFromId(structureId) != null;
    }

    return village.getWorkplaceFromId(structureId) != null;
  }

  private void logSkippedStagedPlanning(StructureType structureType) {
    ProjectCivicCore.LOGGER.atInfo()
        .log("[MakeVillageAssessment] Skipping new " + structureType +
            " project because a staged construction is already registered in topology");
  }

  private VillageProject buildSpawnVillagerProject(short currentLevel) {
    VillageProject project = new VillageProject();
    project.setProjectId(UUID.randomUUID());
    project.setObjectiveType(VillageObjectiveType.SPAWN_VILLAGER);
    project.setObjectiveStatus(VillageObjectiveStatus.NEW);
    project.setWeight(Weight.HIGH);
    project.setTotalStages(VillageObjectiveType.SPAWN_VILLAGER.getStages());
    project.setRequiredMaterialPerStage(getMaterialQuantitiesPerStage(
        VillageNewVillagerCostRegistry.getRequirementsForLevel(currentLevel),
        project.getTotalStages()));

    return project;
  }

  private VillageProject buildAssignHouseProject() {
    VillageProject project = new VillageProject();
    project.setProjectId(UUID.randomUUID());
    project.setObjectiveType(VillageObjectiveType.ASSIGN_HOUSE);
    project.setObjectiveStatus(VillageObjectiveStatus.NEW);
    project.setWeight(Weight.TOP);
    project.setTotalStages(VillageObjectiveType.ASSIGN_HOUSE.getStages());
    project.setRequiredMaterialPerStage(new MaterialQuantity[0]);
    return project;
  }

  private VillageProject buildAssignWorkerProject() {
    VillageProject project = new VillageProject();
    project.setProjectId(UUID.randomUUID());
    project.setObjectiveType(VillageObjectiveType.ASSIGN_WORKER);
    project.setObjectiveStatus(VillageObjectiveStatus.NEW);
    project.setWeight(Weight.TOP);
    project.setTotalStages(VillageObjectiveType.ASSIGN_WORKER.getStages());
    project.setRequiredMaterialPerStage(new MaterialQuantity[0]);
    return project;
  }

  private VillageProject buildNewUpgradeProject(StructureType structureType, short level, UUID structureId) {
    VillageProject project = new VillageProject();
    project.setProjectId(UUID.randomUUID());
    project.setObjectiveType(VillageObjectiveType.UPGRADE_BUILDING);
    project.setObjectiveStatus(VillageObjectiveStatus.NEW);
    project.setWeight(Weight.LOW);
    project.setTargetLevel(level);
    project.setTotalStages(VillageObjectiveType.getStages(structureType));
    project.setStructureType(structureType);
    project.setStructureId(structureId);
    project.setRequiredMaterialPerStage(getMaterialQuantitiesPerStage(
        VillageBuildingUpgradeCostRegistry.getRequirementsForNextLevel(structureType, level),
        project.getTotalStages()));

    return project;
  }

  private VillageProject buildNewWorkplaceProject(StructureType structureType) {
    VillageProject project = new VillageProject();
    project.setProjectId(UUID.randomUUID());
    project.setObjectiveType(VillageObjectiveType.BUILD_WORKPLACE);
    project.setObjectiveStatus(VillageObjectiveStatus.NEW);
    project.setWeight(Weight.MID);
    project.setTargetLevel(1);
    project.setTotalStages(VillageObjectiveType.BUILD_WORKPLACE.getStages());
    project.setStructureType(structureType);
    project.setStructureId(UUID.randomUUID());
    project.setRequiredMaterialPerStage(getMaterialQuantitiesPerStage(
        VillageBuildingCostRegistry.getRequirementsForBuilding(structureType),
        project.getTotalStages()));

    return project;
  }

  private VillageProject buildNewHouseProject() {
    VillageProject project = new VillageProject();
    project.setProjectId(UUID.randomUUID());
    project.setObjectiveType(VillageObjectiveType.BUILD_HOUSE);
    project.setObjectiveStatus(VillageObjectiveStatus.NEW);
    project.setWeight(Weight.HIGH);
    project.setTargetLevel(1);
    project.setTotalStages(VillageObjectiveType.BUILD_HOUSE.getStages());
    project.setStructureType(StructureType.HOUSE);
    project.setStructureId(UUID.randomUUID());
    project.setRequiredMaterialPerStage(getMaterialQuantitiesPerStage(
        VillageBuildingCostRegistry.getRequirementsForBuilding(StructureType.HOUSE),
        project.getTotalStages()));
    return project;
  }

  private VillageProject buildUpdateVillageProject(short level) {
    VillageProject project = new VillageProject();
    project.setProjectId(UUID.randomUUID());
    project.setObjectiveType(VillageObjectiveType.VILLAGE_LEVEL_UP);
    project.setObjectiveStatus(VillageObjectiveStatus.NEW);
    project.setWeight(Weight.HIGH);
    project.setTargetLevel(level);
    project.setTotalStages(VillageObjectiveType.VILLAGE_LEVEL_UP.getStages());
    project.setRequiredMaterialPerStage(getMaterialQuantitiesPerStage(
        VillageLevelUpCostRegistry.getRequirementsForNextLevel(level),
        project.getTotalStages()));
    return project;
  }

  private MaterialQuantity[] getMaterialQuantitiesPerStage(
      Set<MaterialQuantity> totalQuantities,
      short stages) {
    List<MaterialQuantity> singleStageMaterial = new ArrayList<>();

    for (MaterialQuantity material : totalQuantities) {
      singleStageMaterial
          .add(new MaterialQuantity(
              material.getItemId(), null, null, (int) material.getQuantity() / stages, null));
    }

    return singleStageMaterial.toArray(MaterialQuantity[]::new);
  }

  private record VillageLimits(
      short maxVillager,
      Map<StructureType, Short> structureMaxCount,
      Map<StructureType, Short> maxStructureLevel) {
  }
}
