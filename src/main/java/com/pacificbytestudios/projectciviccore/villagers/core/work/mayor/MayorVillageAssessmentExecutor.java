package com.pacificbytestudios.projectciviccore.villagers.core.work.mayor;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageBuilderComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerEntryComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequestTracker;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.TownHallComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan.VillageProject;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.EnqueueVillageStorageRequestTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.LevelUpVillageTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnVillagerTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.TryHouseAssignmentTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.TryJobAssignmentTask;
import com.pacificbytestudios.projectciviccore.village.enums.VillageObjectiveStatus;
import com.pacificbytestudios.projectciviccore.village.enums.VillageStorageRequestType;
import com.pacificbytestudios.projectciviccore.villagers.components.MayorComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver.ResolvedContainer;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryItemCriteria;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryTransferValidators;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class MayorVillageAssessmentExecutor {

  private World world;
  private boolean creative;

  public boolean executeNext(
      World world,
      VillageHeartComponent village,
      TownHallComponent townHall,
      MayorComponent mayorComponent) {
    return executeNext(world, village, townHall, mayorComponent, false);
  }

  public boolean executeNext(
      World world,
      VillageHeartComponent village,
      TownHallComponent townHall,
      MayorComponent mayorComponent,
      boolean creative) {
    if (world == null || village == null || townHall == null || mayorComponent == null) {
      return false;
    }

    if (mayorComponent.getCurrentProject() == null) {
      VillageProjectPlan executingPlan = mayorComponent.getExecutingVillagePlan();
      if (executingPlan == null) {
        return false;
      }

      mayorComponent.setCurrentProject(
          executingPlan.pollNextProject());
    }

    this.world = world;
    this.creative = creative;
    return handleCurrentProject(village, mayorComponent, townHall);
  }

  private boolean handleCurrentProject(
      VillageHeartComponent village,
      MayorComponent mayorComponent,
      TownHallComponent townHall) {
    VillageProject project = mayorComponent.getCurrentProject();
    if (project == null) {
      return false;
    }

    try {
      switch (project.getObjectiveType()) {
        case ASSIGN_HOUSE:
          handleAssignHouseProject(village, townHall, mayorComponent, project);
          return true;
        case ASSIGN_WORKER:
          handleAssignWorkerProject(village, townHall, mayorComponent, project);
          return true;
        case SPAWN_VILLAGER:
          handleSpawnVillagerProject(village, townHall, mayorComponent, project);
          return true;
        case BUILD_HOUSE:
        case BUILD_WORKPLACE:
        case UPGRADE_BUILDING:
          handleBuilderProject(village, townHall, mayorComponent, project);
          return true;
        case BUILD_DEFENCE:
          handleBuildDefenceProject(village, townHall, mayorComponent, project);
          return true;
        case VILLAGE_LEVEL_UP:
          handleLevelUpVillageProject(village, townHall, mayorComponent, project);
          return true;
        case DEMOLISH:
          handleDemolishProject(village, townHall, mayorComponent, project);
          return true;
        default:
          throw new IllegalArgumentException(
              "No handlers declared for objective type: "
                  + project.getObjectiveType());
      }
    } catch (Exception e) {
      Logger.logWarning("Could not execute project "
          + project.getProjectId() + ". REASON:\n" + e.getMessage());
      e.printStackTrace();
      mayorComponent.setCurrentProject(null);
      return false;
    }
  }

  private void handleAssignHouseProject(
      VillageHeartComponent village,
      TownHallComponent townHall,
      MayorComponent mayorComponent,
      VillageProject project) {
    HouseComponent house = village.findHouseWithVacancy();
    project.setObjectiveStatus(VillageObjectiveStatus.COMPLETED);
    syncProjectState(townHall, project);
    townHall.removeProject(project);
    mayorComponent.setCurrentProject(null);

    if (house == null || village.getHomelessCount() == 0) {
      return;
    }

    VillageTaskQueueComponent queueComponent = new VillageTaskQueueComponent();
    queueComponent.registerTask(new TryHouseAssignmentTask(house.getUuid()));
    this.world.execute(() -> Utils.safeVillageTaskEnqueue(
        village.getTownHall().getVillageHeartRef(),
        queueComponent));
  }

  private void handleAssignWorkerProject(
      VillageHeartComponent village,
      TownHallComponent townHall,
      MayorComponent mayorComponent,
      VillageProject project) {
    UUID workplaceId = village.getFirstWorkplaceWithMissingEmployee();
    project.setObjectiveStatus(VillageObjectiveStatus.COMPLETED);
    syncProjectState(townHall, project);
    townHall.removeProject(project);
    mayorComponent.setCurrentProject(null);

    if (workplaceId == null || village.getUnemployedRegistry().isEmpty()) {
      return;
    }

    VillageTaskQueueComponent queueComponent = new VillageTaskQueueComponent();
    queueComponent.registerTask(new TryJobAssignmentTask(workplaceId));
    this.world.execute(() -> Utils.safeVillageTaskEnqueue(
        village.getTownHall().getVillageHeartRef(),
        queueComponent));
  }

  private void handleSpawnVillagerProject(
      VillageHeartComponent village,
      TownHallComponent townHall,
      MayorComponent mayorComponent,
      VillageProject project) {
    HouseComponent availableHouse = village.findHouseWithVacancy();
    if (availableHouse == null) {
      Logger.logWarning("Removing villager spawn project "
          + project.getProjectId() + " because no house has a vacancy");
      townHall.removeProject(project);
      mayorComponent.setCurrentProject(null);
      return;
    }

    if (!processProjectStageWithResources(village, project)) {
      syncProjectState(townHall, project);
      mayorComponent.setCurrentProject(null);
      return;
    }

    project.setObjectiveStatus(VillageObjectiveStatus.COMPLETED);
    syncProjectState(townHall, project);

    SpawnVillagerTask spawnTask = new SpawnVillagerTask(
        village.getNextRequiredRole().getRole(),
        3);
    TryHouseAssignmentTask homeAssignmentTask = new TryHouseAssignmentTask(
        availableHouse.getUuid());

    VillageTaskQueueComponent queueComponent = new VillageTaskQueueComponent();
    queueComponent.registerTask(spawnTask);
    queueComponent.registerTask(homeAssignmentTask);

    UUID workplaceId = village.getFirstWorkplaceWithMissingEmployee();
    if (workplaceId != null) {
      TryJobAssignmentTask jobAssignmentTask = new TryJobAssignmentTask(workplaceId);
      queueComponent.registerTask(jobAssignmentTask);
    }

    townHall.removeProject(project);
    mayorComponent.setCurrentProject(null);

    this.world.execute(() -> {
      Utils.safeVillageTaskEnqueue(
          village.getTownHall().getVillageHeartRef(),
          queueComponent);
    });
  }

  private void handleBuildDefenceProject(
      VillageHeartComponent village,
      TownHallComponent townHall,
      MayorComponent mayorComponent,
      VillageProject project) {
    Logger.logSevere("Unimplemented method 'handleBuildDefenceProject'");
    mayorComponent.setCurrentProject(null);
  }

  private void handleLevelUpVillageProject(
      VillageHeartComponent village,
      TownHallComponent townHall,
      MayorComponent mayorComponent,
      VillageProject project) {
    if (!processProjectStageWithResources(village, project)) {
      syncProjectState(townHall, project);
      mayorComponent.setCurrentProject(null);
      return;
    }

    project.setObjectiveStatus(VillageObjectiveStatus.COMPLETED);
    syncProjectState(townHall, project);

    LevelUpVillageTask levelUpTask = new LevelUpVillageTask((short) project.getTargetLevel());

    VillageTaskQueueComponent queueComponent = new VillageTaskQueueComponent();
    queueComponent.registerTask(levelUpTask);
    townHall.removeProject(project);
    mayorComponent.setCurrentProject(null);

    this.world.execute(() -> {
      Utils.safeVillageTaskEnqueue(
          village.getTownHall().getVillageHeartRef(),
          queueComponent);
    });
  }

  private void handleDemolishProject(
      VillageHeartComponent village,
      TownHallComponent townHall,
      MayorComponent mayorComponent,
      VillageProject project) {
    Logger.logSevere("Unimplemented method 'handleDemolishProject'");
    mayorComponent.setCurrentProject(null);
  }

  private void handleBuilderProject(
      VillageHeartComponent village,
      TownHallComponent townHall,
      MayorComponent mayorComponent,
      VillageProject project) {
    if (project.getObjectiveStatus() != VillageObjectiveStatus.NEW &&
        !processProjectStageWithResources(village, project)) {
      syncProjectState(townHall, project);
      mayorComponent.setCurrentProject(null);
      return;
    }

    if (project.getStructureId() == null) {
      project.setStructureId(UUID.randomUUID());
    }

    if (project.getObjectiveStatus() != VillageObjectiveStatus.NEW) {
      syncProjectState(townHall, project);
    }

    mayorComponent.setCurrentProject(null);

    this.world.execute(() -> {
      queueProjectForVillageBuilder(village, project);
    });
  }

  private void queueProjectForVillageBuilder(
      VillageHeartComponent village,
      VillageProject project) {
    Ref<ChunkStore> villageHeartRef = village.getTownHall().getVillageHeartRef();
    if (villageHeartRef == null || !villageHeartRef.isValid()) {
      Logger.logWarning("Could not queue builder project without a valid village ref");
      return;
    }

    Store<ChunkStore> chunkStore = villageHeartRef.getStore();
    VillageBuilderComponent builder = village.getComponent(
        VillageBuilderComponent.getComponentType());

    boolean createdBuilder = false;
    if (builder == null) {
      builder = new VillageBuilderComponent();
      builder.setProjectPlan(new VillageProjectPlan());
      createdBuilder = true;
    } else if (builder.getProjectPlan() == null) {
      builder.setProjectPlan(new VillageProjectPlan());
    }

    builder.addProject(project);

    if (createdBuilder) {
      chunkStore.putComponent(
          villageHeartRef,
          VillageBuilderComponent.getComponentType(),
          builder);
    }
  }

  private boolean processProjectStageWithResources(
      VillageHeartComponent village,
      VillageProject project) {
    if (this.creative) {
      project.setObjectiveStatus(VillageObjectiveStatus.EXECUTING);
      return true;
    }

    WorkplaceComponent townHallWorkplace = village.getTownHall();
    SimpleItemContainer container = getTownHallContainer(townHallWorkplace);
    List<InventoryItemCriteria> requiredMaterials = buildItemCriteriaFromMaterial(project);
    List<InventoryItemCriteria> missingMaterials = InventoryTransferValidators.getMissingCriteria(
        container,
        requiredMaterials);

    if (!missingMaterials.isEmpty()) {
      project.setObjectiveStatus(VillageObjectiveStatus.RESOURCE_GATHERING);
      queueProjectMaterialRequestIfNeeded(village, townHallWorkplace, missingMaterials);
      return false;
    }

    if (!InventoryTransferValidators.removeRequestedItems(container, requiredMaterials)) {
      Logger.logWarning("Could not consume requested materials for project "
          + project.getProjectId());
      project.setObjectiveStatus(VillageObjectiveStatus.RESOURCE_GATHERING);
      return false;
    }

    project.setObjectiveStatus(VillageObjectiveStatus.EXECUTING);
    return true;
  }

  private void syncProjectState(
      TownHallComponent townHall,
      VillageProject project) {
    if (townHall == null || project == null) {
      return;
    }

    boolean synced = townHall.updateProjectPlan(project);
    if (synced) {
      Logger.logInfo("Synced project " + project.getProjectId()
          + " status=" + project.getObjectiveStatus()
          + " stage=" + project.getStage() + "/" + project.getTotalStages()
          + " structureId=" + project.getStructureId());
      return;
    }

    Logger.logWarning("Could not sync project " + project.getProjectId()
        + " back to the town hall plan");
  }

  private SimpleItemContainer getTownHallContainer(WorkplaceComponent townHallWorkplace) {
    WorkplaceContainerEntryComponent container = townHallWorkplace.getStorage().getFirstContainer();
    if (container == null) {
      throw new IllegalStateException("Town Hall registered without a valid storage");
    }

    ResolvedContainer resolvedContainer = ContainerResolver.resolveContainerAt(
        this.world,
        container.getPosition());
    if (resolvedContainer == null) {
      throw new IllegalStateException("Town Hall storage not found in the world");
    }

    return resolvedContainer.containerBlock().getItemContainer();
  }

  private List<InventoryItemCriteria> buildItemCriteriaFromMaterial(VillageProject project) {
    return Arrays.stream(project.getRequiredMaterialPerStage())
        .filter(material -> material != null)
        .map(InventoryItemCriteria::fromMaterialQuantity)
        .toList();
  }

  private void queueProjectMaterialRequestIfNeeded(
      VillageHeartComponent village,
      WorkplaceComponent townHallWorkplace,
      List<InventoryItemCriteria> missingMaterials) {
    Ref<ChunkStore> villageHeartRef = townHallWorkplace.getVillageHeartRef();
    if (villageHeartRef == null || !villageHeartRef.isValid()) {
      Logger.logWarning("Could not queue a material request without a valid village ref");
      return;
    }

    boolean hasPendingDeliverRequest = VillageStorageRequestTracker.hasPendingRequest(
        villageHeartRef,
        village,
        townHallWorkplace.getUuid(),
        VillageStorageRequestType.DELIVER_TO_WORKPLACE);
    if (hasPendingDeliverRequest) {
      return;
    }

    VillageStorageRequest request = buildTownHallMaterialRequest(
        townHallWorkplace,
        missingMaterials);
    if (request.getItemCriteria() == null || request.getItemCriteria().isEmpty()) {
      return;
    }

    EnqueueVillageStorageRequestTask task = new EnqueueVillageStorageRequestTask(request);
    task.setOnSuccessCallback(requestResult -> {
      String[] requestParts = requestResult.split(":", 2);
      UUID storageId = UUID.fromString(requestParts[0]);
      request.setStorageWorkplaceId(storageId);
      request.addTriedStorageId(storageId);
      request.overrideRequestId(UUID.fromString(requestParts[1]));
      request.setRegisteredWithStorage(true);
      syncLastStorageRequest(villageHeartRef, village, townHallWorkplace, request);
    });
    task.setOnError((status, message) -> syncLastStorageRequest(villageHeartRef, village, townHallWorkplace, null));

    VillageTaskQueueComponent queueComponent = new VillageTaskQueueComponent();
    queueComponent.registerTask(task);

    this.world.execute(() -> Utils.safeVillageTaskEnqueue(villageHeartRef, queueComponent));
  }

  private VillageStorageRequest buildTownHallMaterialRequest(
      WorkplaceComponent townHallWorkplace,
      List<InventoryItemCriteria> missingMaterials) {
    VillageStorageRequest request = new VillageStorageRequest();
    request.setType(VillageStorageRequestType.DELIVER_TO_WORKPLACE);
    request.setRequesterWorkplaceId(townHallWorkplace.getUuid());
    request.setTargetsWorkplaceStorage(
        townHallWorkplace.getStorage().getAllContainersTargets());

    for (InventoryItemCriteria materialCriteria : missingMaterials) {
      if (!materialCriteria.isFilterIn() || materialCriteria.getCount() <= 0) {
        continue;
      }

      request.addItemCriteria(materialCriteria.toVillageRequestCriterion());
    }

    return request;
  }

  private void syncLastStorageRequest(
      Ref<ChunkStore> villageHeartRef,
      VillageHeartComponent village,
      WorkplaceComponent townHallWorkplace,
      VillageStorageRequest request) {
    setLastStorageRequest(townHallWorkplace, request);
    setLastStorageRequest(village.getTownHall(), request);

    Ref<ChunkStore> townHallRef = townHallWorkplace.getWorkplaceRef().getRef();
    WorkplaceComponent persistedTownHall = townHallRef.getStore().getComponent(
        townHallRef,
        WorkplaceComponent.getComponentType());
    if (persistedTownHall == null) {
      throw new IllegalStateException("Town Hall workplace component not found in chunk store");
    }

    setLastStorageRequest(persistedTownHall, request);
    markNeedsSaving(townHallRef, persistedTownHall);

    villageHeartRef.getStore().putComponent(
        villageHeartRef,
        VillageHeartComponent.getComponentType(),
        village);
  }

  private void setLastStorageRequest(
      WorkplaceComponent townHallWorkplace,
      VillageStorageRequest request) {
    townHallWorkplace.setLastStorageRequest(request != null ? request.clone() : null);
  }

  private void markNeedsSaving(
      Ref<ChunkStore> townHallRef,
      WorkplaceComponent townHallWorkplace) {
    Store<ChunkStore> chunkStore = townHallRef.getStore();
    chunkStore.putComponent(
        townHallRef,
        WorkplaceComponent.getComponentType(),
        townHallWorkplace);

    BlockModule.BlockStateInfo blockStateInfo = chunkStore.getComponent(
        townHallRef,
        BlockModule.BlockStateInfo.getComponentType());
    if (blockStateInfo != null) {
      blockStateInfo.markNeedsSaving();
    }
  }
}
