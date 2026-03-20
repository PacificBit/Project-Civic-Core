package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.mayor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.registry.registry.VillageBuildingCostRegistry;
import com.pacificbytestudios.projectciviccore.registry.registry.VillageBuildingUpgradeCostRegistry;
import com.pacificbytestudios.projectciviccore.registry.registry.VillageLevelUpCostRegistry;
import com.pacificbytestudios.projectciviccore.registry.registry.VillageNewVillagerCostRegistry;
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
import com.pacificbytestudios.projectciviccore.villagers.components.MayorComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;

public class MakeVillageAssessment extends VillagerActionBase {

  private static final int MAX_NEW_BUILD = 2;
  private static final int MAX_UPGRADE_BUILD = 2;
  private static final StructureType[] WORKPLACE_BUILD_PRIORITY = new StructureType[] {
      StructureType.STORAGE, StructureType.FARM,
      StructureType.TAVERN, StructureType.SAWMILL, StructureType.MINE };
  private static final StructureType[] STRUCTURE_UPGRADE_PRIORITY = new StructureType[] {
      StructureType.STORAGE, StructureType.FARM, StructureType.TAVERN,
      StructureType.SAWMILL, StructureType.MINE, StructureType.TOWN_HALL };

  private record VillageLimits(short maxVillager,
      Map<StructureType, Short> structureMaxCount,
      Map<StructureType, Short> maxStructureLevel) {
  }

  public MakeVillageAssessment(BuilderActionBase builderActionBase) {
    super(builderActionBase);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.canExecute(ref, role, sensorInfo, deltaTime, store)) {
      return false;
    }
    super.execute(ref, role, sensorInfo, deltaTime, store);

    MayorComponent mayorComponent = this.getMayorComponent(store, ref);

    TownHallComponent townHall = this.getComponentFromWorkplace(store, ref, TownHallComponent.getComponentType());
    VillageHeartComponent village = this.getVillageHeartComponent(ref, store);
    if (townHall == null) {
      return false;
    }

    VillageProjectPlan projectPlan = townHall.getVillageProjectPlan();
    if (projectPlan == null) {
      projectPlan = new VillageProjectPlan();
      townHall.setVillageProjectPlan(projectPlan);
    }

    short villageLevel = (short) village.getLevel();
    VillageHeartConfigComponent config = village.getConfig();

    VillageLimits villageLimits = new VillageLimits(
        config.getVillagerCapPerLevel(villageLevel),
        config.getStructureLimitsPerLevel(villageLevel),
        config.getStructureMaxLevelPerLevel(villageLevel));

    Map<StructureType, Short> structureCount = village.getStructureTypeCount();

    WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
    townHall.setLastDayVillageAssessment(worldTimeResource.getGameDateTime().getDayOfYear());

    // Step 0: nothing in queue and cannot create new buildings or upgrade old ones
    if (!projectPlan.hasProjects() &&
        villageLevel < config.getMaxLevel() &&
        isStructureLimitReached(villageLimits, structureCount) &&
        areStructuresMaxedOut(villageLimits, village.getWorkplaces(), village.getHouses())) {
      VillageProject project = buildUpdateVillageProject((short) (villageLevel + 1));
      projectPlan.addProject(project);
      mayorComponent.setExecutingVillagePlan(projectPlan);
      return true;
    }

    if (projectPlan.contains(VillageObjectiveType.VILLAGE_LEVEL_UP)) {
      return true;
    }

    // Step 1: check for the real necessities
    populateHighPriorityProjects(projectPlan, villageLimits, village);
    // Step 2: check for missing structures
    populateMidPriorityProjects(projectPlan, villageLimits, village, structureCount);
    // Step 3: check for possible upgrades
    populateLowPriorityProjects(projectPlan, villageLimits, village);

    ProjectCivicCore.LOGGER.atInfo().log("CreatedPlan:");
    ProjectCivicCore.LOGGER.atInfo().log(projectPlan.toString());

    mayorComponent.setExecutingVillagePlan(projectPlan);
    return true;
  }

  private void populateHighPriorityProjects(
      VillageProjectPlan plan,
      VillageLimits limits,
      VillageHeartComponent village) {

    if (requireNewHouses(plan, limits, village)) {
      VillageProject project = buildNewHouseProject();
      plan.addProject(project);
      return;
    }

    if (village.getAvailableBedsCount() > 0 &&
        village.getVillagerCount() < limits.maxVillager &&
        !plan.contains(VillageObjectiveType.SPAWN_VILLAGER)) {
      VillageProject project = buildSpawnVillagerProject((short) village.getLevel());
      plan.addProject(project);
    }
  }

  private void populateMidPriorityProjects(
      VillageProjectPlan plan,
      VillageLimits limits,
      VillageHeartComponent village,
      Map<StructureType, Short> structureCount) {

    short plannedNewWorkplace = plan.getObjectiveTypeCount(VillageObjectiveType.BUILD_WORKPLACE);
    if (plannedNewWorkplace < MAX_NEW_BUILD) {
      for (StructureType structureType : WORKPLACE_BUILD_PRIORITY) {
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

    // TODO: If there is nothing to build, see if can build defences
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

    // TODO: If there is no upgrade of workplace see if can upgrade defences
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

  private boolean requireNewHouses(VillageProjectPlan plan, VillageLimits limits, VillageHeartComponent village) {
    return village.getHouseCount() < limits.structureMaxCount.get(StructureType.HOUSE) &&
        !plan.contains(VillageObjectiveType.BUILD_HOUSE) &&
        (village.getHomelessCount() != 0 || village.getAvailableBedsCount() == 0);
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

  private VillageProject buildNewUpgradeProject(StructureType structureType, short level, UUID structureId) {
    VillageProject project = new VillageProject();
    project.setProjectId(UUID.randomUUID());
    project.setObjectiveType(VillageObjectiveType.UPGRADE_BUILDING);
    project.setObjectiveStatus(VillageObjectiveStatus.NEW);
    project.setWeight(Weight.LOW);
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
    project.setTotalStages(VillageObjectiveType.BUILD_WORKPLACE.getStages());
    project.setStructureType(structureType);
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
    project.setTotalStages(VillageObjectiveType.BUILD_HOUSE.getStages());
    project.setStructureType(StructureType.HOUSE);
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
}
