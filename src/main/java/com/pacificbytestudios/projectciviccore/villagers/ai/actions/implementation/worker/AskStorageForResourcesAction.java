package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.farm.FarmComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.sawmill.SawmillComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequest.ItemRequestCriterion;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.VillageStorageRequestTracker;
import com.pacificbytestudios.projectciviccore.village.components.workplace.tavern.TavernComponent;
import com.pacificbytestudios.projectciviccore.village.core.interfaces.IWorkplaceMissingResource;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.EnqueueVillageStorageRequestTask;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageStorageRequestType;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.BuilderAskStorageForResourcesAction;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryItemCriteria;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;

public class AskStorageForResourcesAction extends VillagerActionBase {
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
      return false;
    }
    super.execute(ref, role, sensorInfo, deltaTime, store);

    WorkplaceComponent workplaceComponent = this.getWorkplaceComponent(ref, store);
    Ref<ChunkStore> villageHeartRef = this.getVillageRef(ref, store);
    Ref<ChunkStore> workplaceRef = this.getWorkplaceRef(ref, store);
    VillageHeartComponent villageHeart = this.getVillageHeartComponent(ref, store);
    if (workplaceComponent == null || villageHeartRef == null || workplaceRef == null || villageHeart == null) {
      return false;
    }

    boolean hasPendingDeliverRequest = VillageStorageRequestTracker.hasPendingRequest(
        villageHeartRef,
        villageHeart,
        workplaceComponent.getUuid(),
        VillageStorageRequestType.DELIVER_TO_WORKPLACE);

    if (hasPendingDeliverRequest) {
      ProjectCivicCore.LOGGER.atInfo()
          .log("[AskStorageForResourcesAction] Deliver request already present");
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

    syncLastStorageRequest(villageHeartRef, villageHeart, workplaceRef, workplaceComponent, request);

    EnqueueVillageStorageRequestTask task = new EnqueueVillageStorageRequestTask(request);
    task.setOnSuccessCallback(requestId -> {
      request.overrideRequestId(UUID.fromString(requestId));
      syncLastStorageRequest(villageHeartRef, villageHeart, workplaceRef, workplaceComponent, request);
    });
    task.setOnError((status, message) -> syncLastStorageRequest(villageHeartRef, villageHeart, workplaceRef,
        workplaceComponent, null));

    VillageTaskQueueComponent taskComponent = new VillageTaskQueueComponent();
    taskComponent.registerTask(task);
    Utils.safeVillageTaskEnqueue(villageHeartRef, taskComponent);
    return true;
  }

  private VillageStorageRequest buildClearRequest(WorkplaceComponent workplace) {
    VillageStorageRequest request = new VillageStorageRequest();
    request.setType(VillageStorageRequestType.DELIVER_TO_WORKPLACE);
    request.setRequesterWorkplaceId(workplace.getUuid());

    if (this.criteriaSourceType.equals(SourceType.MANUAL)) {
      buildManualCriteria(request);
    } else {
      buildWorkplaceItemCriteria(request, workplace);
    }

    request.setTargetsWorkplaceStorage(workplace.getStorage().getAllContainerAccessPositions());
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

  private void buildWorkplaceItemCriteria(
      VillageStorageRequest request,
      WorkplaceComponent workplace) {

    Ref<ChunkStore> workplaceRef = workplace.getWorkplaceRef().getRef();
    Store<ChunkStore> chunkStore = workplaceRef.getStore();

    IWorkplaceMissingResource missingResourceComponent = require(getProperComponent(
        chunkStore,
        workplaceRef,
        workplace.getWorkplaceType()), "missing workplace resource component");
    if (missingResourceComponent == null) {
      return;
    }

    Set<ItemRequestCriterion> criterions = missingResourceComponent.getMissingResources();
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

  private void syncLastStorageRequest(
      Ref<ChunkStore> villageHeartRef,
      VillageHeartComponent villageHeart,
      Ref<ChunkStore> workplaceRef,
      WorkplaceComponent sourceWorkplace,
      VillageStorageRequest request) {
    applyLastStorageRequest(sourceWorkplace, request);

    if (villageHeart != null && sourceWorkplace != null) {
      WorkplaceComponent registeredWorkplace = villageHeart.getWorkplaceFromId(sourceWorkplace.getUuid());
      applyLastStorageRequest(registeredWorkplace, request);
    }

    if (workplaceRef != null && workplaceRef.isValid()) {
      WorkplaceComponent persistedWorkplace = workplaceRef.getStore().getComponent(
          workplaceRef,
          WorkplaceComponent.getComponentType());
      applyLastStorageRequest(persistedWorkplace, request);
      persistWorkplaceComponent(workplaceRef, persistedWorkplace);
    }

    if (villageHeartRef != null && villageHeart != null) {
      villageHeartRef.getStore().putComponent(
          villageHeartRef,
          VillageHeartComponent.getComponentType(),
          villageHeart);
    }
  }

  private void applyLastStorageRequest(WorkplaceComponent workplace, VillageStorageRequest request) {
    if (workplace == null) {
      return;
    }

    workplace.setLastStorageRequest(request != null ? request.clone() : null);
  }

  private IWorkplaceMissingResource getProperComponent(
      Store<ChunkStore> chunkStore,
      Ref<ChunkStore> ref,
      StructureType workplaceType) {
    return switch (workplaceType) {
      case TAVERN -> chunkStore.getComponent(ref, TavernComponent.getComponentType());
      case FARM -> chunkStore.getComponent(ref, FarmComponent.getComponentType());
      case SAWMILL -> chunkStore.getComponent(ref, SawmillComponent.getComponentType());
      case MINE -> chunkStore.getComponent(ref, MineComponent.getComponentType());
      default -> throw new IllegalArgumentException("Workplace type '" + workplaceType + "' not supported!");
    };
  }

  private void persistWorkplaceComponent(Ref<ChunkStore> workplaceRef, WorkplaceComponent workplace) {
    if (workplaceRef == null || !workplaceRef.isValid() || workplace == null) {
      return;
    }

    Store<ChunkStore> chunkStore = workplaceRef.getStore();
    chunkStore.putComponent(workplaceRef, WorkplaceComponent.getComponentType(), workplace);

    BlockModule.BlockStateInfo blockStateInfo = chunkStore.getComponent(
        workplaceRef,
        BlockModule.BlockStateInfo.getComponentType());
    if (blockStateInfo != null) {
      blockStateInfo.markNeedsSaving();
    }
  }
}
