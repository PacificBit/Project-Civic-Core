package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest.ItemRequestCriterion;
import com.pacificbytestudios.projectciviccore.village.components.workplace.tavern.TavernComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.EnqueueVillageStorageRequestTask;
import com.pacificbytestudios.projectciviccore.village.enums.VillageStorageRequestType;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.BuilderAskStorageForResourcesAction;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryItemCriteria;

public class AskStorageForResourcesAction extends ActionBase {
  private List<InventoryItemCriteria> itemCriteria;
  private SourceType criteriaSourceType;

  public AskStorageForResourcesAction(
      BuilderAskStorageForResourcesAction builderActionBase,
      BuilderSupport support) {
    super(builderActionBase);
    this.itemCriteria = builderActionBase.getItemCriteria(support);
    this.criteriaSourceType = builderActionBase.getSourceType(support);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.canExecute(ref, role, sensorInfo, deltaTime, store)) {
      return true;
    }
    super.execute(ref, role, sensorInfo, deltaTime, store);

    VillagerComponent villagerComponent = store.getComponent(ref, VillagerComponent.getComponentType());
    if (villagerComponent == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[AskStorageForResourcesAction] Could not fetch the villager component");
      return false;
    }

    VillagerWorkerComponent workerData = villagerComponent.getWorkerData();
    if (workerData == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[AskStorageForResourcesAction] Could not fetch the villager worker data");
      return false;
    }

    WorkplaceComponent workplaceComponent = workerData.getWorkplace();
    if (workplaceComponent == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[AskStorageForResourcesAction] Could not fetch the workplace component");
      return false;
    }

    if (workplaceComponent.getLastStorageRequest() != null &&
        workplaceComponent.getLastStorageRequest().getType()
            .equals(VillageStorageRequestType.DELIVER_TO_WORKPLACE)) {
      ProjectCivicCore.LOGGER.atInfo()
          .log("[AskStorageForResourcesAction] Deliver request already present");
      return false;
    }

    Ref<ChunkStore> villageHeart = workplaceComponent.getVillageHeartRef();
    if (villageHeart == null || !villageHeart.isValid()) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[AskStorageForResourcesAction] Could not fetch the village heart reference");
      return false;
    }

    VillageStorageRequest request = buildClearRequest(workplaceComponent);
    if (request.getItemCriteria() == null || request.getItemCriteria().isEmpty()) {
      ProjectCivicCore.LOGGER.atWarning()
          .log("[AskStorageForResourcesAction] Refusing to enqueue an empty deliver request for workplace "
              + workplaceComponent.getUuid());
      return false;
    }

    ProjectCivicCore.LOGGER.atInfo()
        .log("[AskStorageForResourcesAction] Built request: " + request);

    EnqueueVillageStorageRequestTask task = new EnqueueVillageStorageRequestTask(request);
    task.setOnSuccessCallback(requestId -> {
      request.overrideRequestId(UUID.fromString(requestId));
      workplaceComponent.setLastStorageRequest(request);
    });

    VillageTaskQueueComponent taskComponent = new VillageTaskQueueComponent();
    taskComponent.registerTask(task);
    Utils.safeVillageTaskEnqueue(villageHeart, taskComponent);
    return true;
  }

  private VillageStorageRequest buildClearRequest(WorkplaceComponent workplace) {
    VillageStorageRequest request = new VillageStorageRequest();
    request.setType(VillageStorageRequestType.DELIVER_TO_WORKPLACE);
    request.setRequesterWorkplaceId(workplace.getUuid());

    if (this.criteriaSourceType.equals(SourceType.MANUAL)) {
      buildManualCriteria(request);
    } else {
      buildWorkplaceCriteria(request, workplace);
    }

    request.setTargetsWorkplaceStorage(workplace.getStorage().getAllContainerPositions());
    return request;
  }

  private void buildManualCriteria(VillageStorageRequest request) {
    for (InventoryItemCriteria requestItem : this.itemCriteria) {
      VillageStorageRequest.ItemRequestCriterion criteria = new VillageStorageRequest.ItemRequestCriterion();
      criteria.setFilterIn(requestItem.isFilterIn());
      criteria.setAttributeKey(requestItem.getItemId());
      criteria.setRequiredAmount(requestItem.getCount());
      request.addItemCriteria(criteria);
    }
  }

  private void buildWorkplaceCriteria(
      VillageStorageRequest request,
      WorkplaceComponent workplace) {
    Ref<ChunkStore> workplaceRef = workplace.getWorkplaceRef().getRef();
    Store<ChunkStore> chunkStore = workplaceRef.getStore();

    switch (workplace.getWorkplaceType()) {
      case TAVERN -> this.buildTavernItemCriteria(request, workplaceRef, chunkStore);
      default -> throw new IllegalArgumentException("Workplace not supported yet: " + workplace.getWorkplaceType());
    }
  }

  private void buildTavernItemCriteria(VillageStorageRequest request, Ref<ChunkStore> ref,
      Store<ChunkStore> chunkStore) {
    TavernComponent tavern = chunkStore.getComponent(ref, TavernComponent.getComponentType());
    if (tavern == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[AskStorageForResourcesAction] Could not fetch tavern component for workplace request");
      return;
    }

    Set<ItemRequestCriterion> criterions = tavern.getMissingResources();
    if (criterions == null || criterions.isEmpty()) {
      ProjectCivicCore.LOGGER.atWarning()
          .log("[AskStorageForResourcesAction] Asked for tavern resources but no missing criteria are present");
      return;
    }

    for (ItemRequestCriterion criterion : criterions) {
      request.addItemCriteria(criterion);
    }
  }

  public enum SourceType implements Supplier<String> {
    MANUAL("manual"),
    WORKPLACE("workplace");

    private final String type;

    SourceType(String type) {
      this.type = type;
    }

    @Override
    public String get() {
      return this.type;
    }
  }
}
