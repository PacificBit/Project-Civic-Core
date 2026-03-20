package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.mayor;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerEntryComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequestTracker;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.TownHallComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.VillageProjectPlan.VillageProject;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.EnqueueVillageStorageRequestTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnVillagerTask;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.TryHouseAssignmentTask;
import com.pacificbytestudios.projectciviccore.village.enums.VillageObjectiveStatus;
import com.pacificbytestudios.projectciviccore.village.enums.VillageStorageRequestType;
import com.pacificbytestudios.projectciviccore.villagers.components.MayorComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.ContainerResolver.ResolvedContainer;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryItemCriteria;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryTransferValidators;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;

public class ExecuteNextProjectStageAction extends VillagerActionBase {

  private World world;

  public ExecuteNextProjectStageAction(BuilderActionBase builderActionBase) {
    super(builderActionBase);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    VillageHeartComponent village = this.getVillageHeartComponent(ref, store);
    TownHallComponent townHall = this.getComponentFromWorkplace(
        store,
        ref,
        TownHallComponent.getComponentType());
    MayorComponent mayorComponent = this.getMayorComponent(store, ref);

    if (village == null || townHall == null) {
      return false;
    }

    if (mayorComponent.getCurrentProject() == null) {
      mayorComponent.setCurrentProject(
          mayorComponent.getExecutingVillagePlan().pollNextProject());
    }

    this.world = store.getExternalData().getWorld();
    handleCurrentProject(village, mayorComponent, townHall);
    return true;
  }

  private void handleCurrentProject(
      VillageHeartComponent village,
      MayorComponent mayorComponent,
      TownHallComponent townHall) {
    VillageProject project = mayorComponent.getCurrentProject();
    if (project == null) {
      return;
    }

    switch (project.getObjectiveType()) {
      case SPAWN_VILLAGER:
        handleSpawnVillagerProject(village, townHall, mayorComponent, project);
        break;
      case BUILD_HOUSE:
        handleBuildHouseProject(village, townHall, mayorComponent, project);
        break;
      case BUILD_WORKPLACE:
        handleBuildWorkplaceProject(village, townHall, mayorComponent, project);
        break;
      case UPGRADE_BUILDING:
        handleUpgradeBuildingProject(village, townHall, mayorComponent, project);
        break;
      case BUILD_DEFENCE:
        handleBuildDefenceProject(village, townHall, mayorComponent, project);
        break;
      case VILLAGE_LEVEL_UP:
        handleLevelUpVillageProject(village, townHall, mayorComponent, project);
        break;
      case DEMOLISH:
        handleDemolishProject(village, townHall, mayorComponent, project);
        break;
      default:
        throw new IllegalArgumentException(
            "[VillageBuildingSystem - handleCurrentProject] No handlers declared for objective type: "
                + project.getObjectiveType());
    }
  }

  private void handleSpawnVillagerProject(
      VillageHeartComponent village,
      TownHallComponent townHall,
      MayorComponent mayorComponent,
      VillageProject project) {
    WorkplaceComponent townHallWorkplace = village.getTownHall();
    SimpleItemContainer container = getTownHallContainer(townHallWorkplace);
    List<InventoryItemCriteria> requiredMaterials = buildItemCriteriaFromMaterial(project);
    List<InventoryItemCriteria> missingMaterials = InventoryTransferValidators.getMissingCriteria(
        container,
        requiredMaterials);

    if (!missingMaterials.isEmpty()) {
      project.setObjectiveStatus(VillageObjectiveStatus.RESOURCE_GATHERING);
      queueProjectMaterialRequestIfNeeded(village, townHallWorkplace, missingMaterials);
      mayorComponent.setCurrentProject(null);
      return;
    }

    if (!InventoryTransferValidators.removeRequestedItems(container, requiredMaterials)) {
      ProjectCivicCore.LOGGER.atWarning()
          .log("[ExecuteNextProjectStageAction] Could not consume requested materials for project "
              + project.getProjectId());
      project.setObjectiveStatus(VillageObjectiveStatus.RESOURCE_GATHERING);
      mayorComponent.setCurrentProject(null);
      return;
    }

    project.setObjectiveStatus(VillageObjectiveStatus.EXECUTING);
    project.setStage((short) (project.getStage() + 1));
    mayorComponent.setCurrentProject(null);

    if (project.getStage() >= project.getTotalStages()) {
      project.setObjectiveStatus(VillageObjectiveStatus.COMPLETED);

      SpawnVillagerTask spawnTask = new SpawnVillagerTask(
          village.getNextRequiredRole().getRole(),
          3);
      TryHouseAssignmentTask homeAssignmentTask = new TryHouseAssignmentTask(
          village.findHouseWithVacancy().getUuid());

      VillageTaskQueueComponent queueComponent = new VillageTaskQueueComponent();
      queueComponent.registerTask(spawnTask);
      queueComponent.registerTask(homeAssignmentTask);
      townHall.removeProject(project);

      this.world.execute(() -> {
        Utils.safeVillageTaskEnqueue(
            village.getTownHall().getVillageHeartRef(),
            queueComponent);
      });
    }
  }

  private void handleBuildHouseProject(
      VillageHeartComponent village,
      TownHallComponent townHall,
      MayorComponent mayorComponent,
      VillageProject project) {
    ProjectCivicCore.LOGGER.atSevere().log("Unimplemented method 'handleBuildHouseProject'");
    mayorComponent.setCurrentProject(null);
  }

  private void handleBuildWorkplaceProject(
      VillageHeartComponent village,
      TownHallComponent townHall,
      MayorComponent mayorComponent,
      VillageProject project) {
    ProjectCivicCore.LOGGER.atSevere().log("Unimplemented method 'handleBuildWorkplaceProject'");
    mayorComponent.setCurrentProject(null);
  }

  private void handleUpgradeBuildingProject(
      VillageHeartComponent village,
      TownHallComponent townHall,
      MayorComponent mayorComponent,
      VillageProject project) {
    ProjectCivicCore.LOGGER.atSevere().log("Unimplemented method 'handleUpgradeBuildingProject'");
    mayorComponent.setCurrentProject(null);
  }

  private void handleBuildDefenceProject(
      VillageHeartComponent village,
      TownHallComponent townHall,
      MayorComponent mayorComponent,
      VillageProject project) {
    ProjectCivicCore.LOGGER.atSevere().log("Unimplemented method 'handleBuildDefenceProject'");
    mayorComponent.setCurrentProject(null);
  }

  private void handleLevelUpVillageProject(
      VillageHeartComponent village,
      TownHallComponent townHall,
      MayorComponent mayorComponent,
      VillageProject project) {
    ProjectCivicCore.LOGGER.atSevere().log("Unimplemented method 'handleLevelUpVillageProject'");
    mayorComponent.setCurrentProject(null);
  }

  private void handleDemolishProject(
      VillageHeartComponent village,
      TownHallComponent townHall,
      MayorComponent mayorComponent,
      VillageProject project) {
    ProjectCivicCore.LOGGER.atSevere().log("Unimplemented method 'handleDemolishProject'");
    mayorComponent.setCurrentProject(null);
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
      ProjectCivicCore.LOGGER.atWarning()
          .log("[ExecuteNextProjectStageAction] Could not queue a material request without a valid village ref");
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

    syncLastStorageRequest(villageHeartRef, village, townHallWorkplace, request);

    EnqueueVillageStorageRequestTask task = new EnqueueVillageStorageRequestTask(request);
    task.setOnSuccessCallback(requestId -> {
      request.overrideRequestId(UUID.fromString(requestId));
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
        townHallWorkplace.getStorage().getAllContainerAccessPositions());

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
