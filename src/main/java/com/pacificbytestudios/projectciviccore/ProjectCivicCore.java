package com.pacificbytestudios.projectciviccore;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.BuilderFactory;
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.codecs.VillageTaskQueueCodecs;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.recipe.components.VillageFoodComponent;
import com.pacificbytestudios.projectciviccore.recipe.registry.CropRecipeRegistry;
import com.pacificbytestudios.projectciviccore.recipe.registry.FoodRecipeRegistry;
import com.pacificbytestudios.projectciviccore.recipe.registry.ToolRecipeRegistry;
import com.pacificbytestudios.projectciviccore.village.commands.VillageCommands;
import com.pacificbytestudios.projectciviccore.village.components.beds.VillageBedComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageSpawnComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.farm.FarmComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerEntryComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerRegistryComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceSpawnComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.WorkplaceVillageStorageRequestComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.tavern.TavernComponent;
import com.pacificbytestudios.projectciviccore.village.systems.heart.VillageHeartLoadUnloadSystem;
import com.pacificbytestudios.projectciviccore.village.systems.heart.VillageHeartPlacementSystem;
import com.pacificbytestudios.projectciviccore.village.systems.villagetask.VillageTaskQueueSystem;
import com.pacificbytestudios.projectciviccore.village.systems.workplace.WorkplaceSpawnSystem;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderAskForClearWorkplaceStorageAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderChangeIntoWorkRoleAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderClearSnapshotGroupPositions;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderEatTavernFoodAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderEquipItemFromInventoryAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderInteractWithExternalInventory;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderMountMountableAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderSetMissingResources;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderUnmountMountableAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.BuilderAskStorageForResourcesAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.BuilderCraftToolAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.BuilderScanWorkAreaAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.BuilderWorkplaceStorageTargetsSnapshotAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.farmer.BuilderCraftSeedsAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.farmer.BuilderHarvestCropAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.farmer.BuilderPlantSeedAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.farmer.BuilderTillSoilAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.farmer.BuilderWaterSoilAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.innkeeper.BuilderCookMealAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.innkeeper.BuilderCreateNewMealPlanAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.innkeeper.BuilderGatherNextMealMaterialFromContainerAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.innkeeper.BuilderPlaceFoodOnPlateAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.storagekeeper.BuilderUpdateRequestStatusAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderBedSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderHasEatenMealTodaySensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderHasStorageRequestInPendingSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderQueryDirtyInventorySensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderReachEmptyTavernSeatSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderTimeRangeSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderWatchForJobAssignment;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderWatchVillageStateSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderHasTargetBlockInSnapshotSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderHasValidWorkAreaSnapshotSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderIsBossWorker;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderReachNextTargetInSnapshotSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderReachWorkplaceSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.innkeeper.BuilderHasNextMealInPlannedSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.innkeeper.BuilderReachPlateToFill;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.innkeeper.BuilderReserveNextPlateToFillSensor;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.BuilderItemCriteria;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryItemCriteria;
import com.pacificbytestudios.projectciviccore.villagers.interactions.VillagerOpenInventoryInteraction;
import com.pacificbytestudios.projectciviccore.villagers.systems.EnsureVillagerComponents;
import com.pacificbytestudios.projectciviccore.villagers.systems.SitOnChairSystem;
import com.pacificbytestudios.projectciviccore.villagers.systems.VillagerInventoryPersistenceSystem;

public final class ProjectCivicCore extends JavaPlugin {
  public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  private static ProjectCivicCore INSTANCE;
  private ComponentType<ChunkStore, VillageHeartComponent> villageCoreComponentType;
  private ComponentType<ChunkStore, VillageSpawnComponent> villageSpawnComponentType;
  private ComponentType<ChunkStore, VillageBedComponent> villageBedComponentType;
  private ComponentType<ChunkStore, VillageTaskQueueComponent> villageTaskQueueComponentType;

  private ComponentType<ChunkStore, ClaimedAreaComponent> claimedAreaComponentType;
  private ComponentType<ChunkStore, TrackedPrefabPlacementComponent> trackedPrefabPlacementComponentType;

  private ComponentType<ChunkStore, WorkplaceComponent> workplaceComponentType;
  private ComponentType<ChunkStore, WorkplaceSpawnComponent> workplaceSpawnComponentType;
  private ComponentType<ChunkStore, WorkplaceContainerRegistryComponent> workplaceContainerRegistryComponentType;
  private ComponentType<ChunkStore, WorkplaceContainerEntryComponent> workplaceContainerEntryComponentType;
  private ComponentType<ChunkStore, WorkplaceVillageStorageRequestComponent> workplaceVillageStorageComponentType;
  private ComponentType<ChunkStore, WorkplaceInteractablesMapComponent> workplaceInteractablesMapComponentType;

  private ComponentType<ChunkStore, VillageFoodComponent> villageFoodComponentType;
  private ComponentType<ChunkStore, TavernComponent> tavernComponentType;
  private ComponentType<ChunkStore, FarmComponent> farmComponentType;

  private ComponentType<EntityStore, VillagerComponent> villagerComponentType;

  public ProjectCivicCore(JavaPluginInit init) {
    super(init);
    INSTANCE = this;
    VillageTaskQueueCodecs.registerAll();
  }

  public static ProjectCivicCore get() {
    return INSTANCE;
  }

  @Override
  protected void setup() {
    super.setup();

    ComponentRegistryProxy<ChunkStore> chunkRegistry = this.getChunkStoreRegistry();
    ComponentRegistryProxy<EntityStore> entityRegistry = this.getEntityStoreRegistry();
    CommandRegistry commandRegistry = this.getCommandRegistry();
    NPCPlugin npcPlugin = NPCPlugin.get();

    this.villageSpawnComponentType = chunkRegistry.registerComponent(
        VillageSpawnComponent.class,
        "VillageSpawnConfig",
        VillageSpawnComponent.CODEC);

    this.villageCoreComponentType = chunkRegistry.registerComponent(
        VillageHeartComponent.class,
        "VillageHeartComponent",
        VillageHeartComponent.CODEC);

    this.villageBedComponentType = chunkRegistry.registerComponent(
        VillageBedComponent.class,
        "VillageBedComponent",
        VillageBedComponent.CODEC);

    this.workplaceComponentType = chunkRegistry.registerComponent(
        WorkplaceComponent.class,
        "WorkplaceComponent",
        WorkplaceComponent.CODEC);

    this.workplaceSpawnComponentType = chunkRegistry.registerComponent(
        WorkplaceSpawnComponent.class,
        "WorkplaceSpawnComponent",
        WorkplaceSpawnComponent.CODEC);

    this.claimedAreaComponentType = chunkRegistry.registerComponent(
        ClaimedAreaComponent.class,
        "ClaimedAreaComponent",
        ClaimedAreaComponent.CODEC);

    this.villageTaskQueueComponentType = chunkRegistry.registerComponent(
        VillageTaskQueueComponent.class,
        "VillageTaskQueueComponent",
        VillageTaskQueueComponent.CODEC);

    this.workplaceContainerRegistryComponentType = chunkRegistry.registerComponent(
        WorkplaceContainerRegistryComponent.class,
        "WorkplaceContainerRegistryComponent",
        WorkplaceContainerRegistryComponent.CODEC);

    this.workplaceContainerEntryComponentType = chunkRegistry.registerComponent(
        WorkplaceContainerEntryComponent.class,
        "WorkplaceContainerEntryComponent",
        WorkplaceContainerEntryComponent.CODEC);

    this.trackedPrefabPlacementComponentType = chunkRegistry.registerComponent(
        TrackedPrefabPlacementComponent.class,
        "TrackedPrefabPlacementComponent",
        TrackedPrefabPlacementComponent.CODEC);

    this.workplaceVillageStorageComponentType = chunkRegistry.registerComponent(
        WorkplaceVillageStorageRequestComponent.class,
        "WorkplaceVillageStorageComponent",
        WorkplaceVillageStorageRequestComponent.CODEC);

    this.villageFoodComponentType = chunkRegistry.registerComponent(
        VillageFoodComponent.class,
        "VillageFoodComponent",
        VillageFoodComponent.CODEC);

    this.tavernComponentType = chunkRegistry.registerComponent(
        TavernComponent.class,
        "TavernComponent",
        TavernComponent.CODEC);

    this.farmComponentType = chunkRegistry.registerComponent(
        FarmComponent.class,
        "FarmComponent",
        FarmComponent.CODEC);

    this.workplaceInteractablesMapComponentType = chunkRegistry.registerComponent(
        WorkplaceInteractablesMapComponent.class,
        "WorkplaceInteractables",
        WorkplaceInteractablesMapComponent.CODEC);

    chunkRegistry.registerSystem(new VillageHeartPlacementSystem());
    chunkRegistry.registerSystem(new VillageHeartLoadUnloadSystem());
    chunkRegistry.registerSystem(new WorkplaceSpawnSystem());
    chunkRegistry.registerSystem(new VillageTaskQueueSystem());

    commandRegistry.registerCommand(new VillageCommands());

    this.villagerComponentType = entityRegistry.registerComponent(
        VillagerComponent.class,
        "VillagerComponent",
        VillagerComponent.CODEC);

    entityRegistry.registerSystem(new EnsureVillagerComponents());
    entityRegistry.registerSystem(new VillagerInventoryPersistenceSystem());
    entityRegistry.registerSystem(new SitOnChairSystem());

    // ===========================
    // Sensors
    // ===========================

    npcPlugin.registerCoreComponentType(
        "WatchVillageState",
        BuilderWatchVillageStateSensor::new);

    npcPlugin.registerCoreComponentType(
        "WatchForJobAssignment",
        BuilderWatchForJobAssignment::new);

    npcPlugin.registerCoreComponentType(
        "ReachBed",
        BuilderBedSensor::new);

    npcPlugin.registerCoreComponentType(
        "ReachWorkplace",
        BuilderReachWorkplaceSensor::new);

    npcPlugin.registerCoreComponentType(
        "TimeInRange",
        BuilderTimeRangeSensor::new);

    npcPlugin.registerCoreComponentType(
        "HasValidWorkAraSnapshot",
        BuilderHasValidWorkAreaSnapshotSensor::new);

    npcPlugin.registerCoreComponentType(
        "HasTargetBlockInSnapshot",
        BuilderHasTargetBlockInSnapshotSensor::new);

    npcPlugin.registerCoreComponentType(
        "ReachNextTargetInSnapshot",
        BuilderReachNextTargetInSnapshotSensor::new);

    npcPlugin.registerCoreComponentType(
        "QueryDirtyInventorySensor",
        BuilderQueryDirtyInventorySensor::new);

    npcPlugin.registerCoreComponentType(
        "IsBossWorker",
        BuilderIsBossWorker::new);

    npcPlugin.registerCoreComponentType(
        "HasANewMealToPrepare",
        BuilderHasNextMealInPlannedSensor::new);

    npcPlugin.registerCoreComponentType(
        "HasStoragePendingRequest",
        BuilderHasStorageRequestInPendingSensor::new);

    npcPlugin.registerCoreComponentType(
        "ReachNextEmptyTavernSeat",
        BuilderReachEmptyTavernSeatSensor::new);

    npcPlugin.registerCoreComponentType(
        "HasEatenMealToday",
        BuilderHasEatenMealTodaySensor::new);

    npcPlugin.registerCoreComponentType(
        "ReserveNextPlateToFill",
        BuilderReserveNextPlateToFillSensor::new);

    npcPlugin.registerCoreComponentType(
        "ReachPlateToFill",
        BuilderReachPlateToFill::new);

    // ===========================
    // Actions
    // ===========================

    npcPlugin.registerCoreComponentType(
        "ChangeIntoWorkRole",
        BuilderChangeIntoWorkRoleAction::new);

    npcPlugin.registerCoreComponentType(
        "ScanWorkArea",
        BuilderScanWorkAreaAction::new);

    npcPlugin.registerCoreComponentType(
        "EquipItemFromInventory",
        BuilderEquipItemFromInventoryAction::new);

    npcPlugin.registerCoreComponentType(
        "TillSoilAction",
        BuilderTillSoilAction::new);

    npcPlugin.registerCoreComponentType(
        "PlantSeedAction",
        BuilderPlantSeedAction::new);

    npcPlugin.registerCoreComponentType(
        "ClearSnapshotGroup",
        BuilderClearSnapshotGroupPositions::new);

    npcPlugin.registerCoreComponentType(
        "HarvestCrop",
        BuilderHarvestCropAction::new);

    npcPlugin.registerCoreComponentType(
        "WaterSoil",
        BuilderWaterSoilAction::new);

    npcPlugin.registerCoreComponentType(
        "InteractWithExternalInventory",
        BuilderInteractWithExternalInventory::new);

    npcPlugin.registerCoreComponentType(
        "SetWorkplaceStorageTargetInSnapshot",
        BuilderWorkplaceStorageTargetsSnapshotAction::new);

    npcPlugin.registerCoreComponentType(
        "UpdateStorageRequestStatus",
        BuilderUpdateRequestStatusAction::new);

    npcPlugin.registerCoreComponentType(
        "AskClearWorkplaceStorage",
        BuilderAskForClearWorkplaceStorageAction::new);

    npcPlugin.registerCoreComponentType(
        "CreateNewMealPlanAction",
        BuilderCreateNewMealPlanAction::new);

    npcPlugin.registerCoreComponentType(
        "GatherNextMealMaterialFromContainer",
        BuilderGatherNextMealMaterialFromContainerAction::new);

    npcPlugin.registerCoreComponentType(
        "AskStorageToDeliveResources",
        BuilderAskStorageForResourcesAction::new);

    npcPlugin.registerCoreComponentType(
        "CookMeal",
        BuilderCookMealAction::new);

    npcPlugin.registerCoreComponentType(
        "PlaceFoodOnPlate",
        BuilderPlaceFoodOnPlateAction::new);

    npcPlugin.registerCoreComponentType(
        "MountMountable",
        BuilderMountMountableAction::new);

    npcPlugin.registerCoreComponentType(
        "EatTavernFood",
        BuilderEatTavernFoodAction::new);

    npcPlugin.registerCoreComponentType(
        "Unmount",
        BuilderUnmountMountableAction::new);

    npcPlugin.registerCoreComponentType(
        "SetMissingResources",
        BuilderSetMissingResources::new);

    npcPlugin.registerCoreComponentType(
        "CraftSeeds",
        BuilderCraftSeedsAction::new);

    npcPlugin.registerCoreComponentType(
        "CraftTool",
        BuilderCraftToolAction::new);

    // ===========================
    // Extra
    // ===========================

    npcPlugin.getBuilderManager().registerFactory(
        new BuilderFactory<>(
            InventoryItemCriteria.class,
            "Type",
            BuilderItemCriteria::new));

    this.getCodecRegistry(Interaction.CODEC).register(
        "VillagerOpenInventoryInteraction",
        VillagerOpenInventoryInteraction.class,
        VillagerOpenInventoryInteraction.CODEC);
  }

  @Override
  protected void start0() {
    super.start0();
    FoodRecipeRegistry.initizalize();
    CropRecipeRegistry.initizalize();
    ToolRecipeRegistry.initizalize();
  }

  public ComponentType<ChunkStore, VillageSpawnComponent> getVillageSpawnComponentType() {
    return this.villageSpawnComponentType;
  }

  public ComponentType<ChunkStore, VillageHeartComponent> getVillageCoreComponentType() {
    return this.villageCoreComponentType;
  }

  public ComponentType<ChunkStore, VillageBedComponent> getVillageBedComponentType() {
    return this.villageBedComponentType;
  }

  public ComponentType<ChunkStore, WorkplaceComponent> getWorkplaceComponentType() {
    return this.workplaceComponentType;
  }

  public ComponentType<ChunkStore, WorkplaceSpawnComponent> getWorkplaceSpawnComponentType() {
    return this.workplaceSpawnComponentType;
  }

  public ComponentType<ChunkStore, ClaimedAreaComponent> getClaimedAreaComponent() {
    return this.claimedAreaComponentType;
  }

  public ComponentType<ChunkStore, TrackedPrefabPlacementComponent> getTrackedPrefabPlacementComponentType() {
    return this.trackedPrefabPlacementComponentType;
  }

  public ComponentType<ChunkStore, VillageTaskQueueComponent> getVillageTaskQueueComponentType() {
    return this.villageTaskQueueComponentType;
  }

  public ComponentType<ChunkStore, WorkplaceContainerRegistryComponent> getWorkplaceStorageComponentType() {
    return this.workplaceContainerRegistryComponentType;
  }

  public ComponentType<ChunkStore, WorkplaceContainerEntryComponent> getWorkplaceContainerEntryComponentType() {
    return this.workplaceContainerEntryComponentType;
  }

  public ComponentType<ChunkStore, WorkplaceVillageStorageRequestComponent> getWorkplaceVillageStorageComponentType() {
    return this.workplaceVillageStorageComponentType;
  }

  public ComponentType<ChunkStore, VillageFoodComponent> getVillageFoodComponentType() {
    return this.villageFoodComponentType;
  }

  public ComponentType<ChunkStore, TavernComponent> getTavernComponentType() {
    return this.tavernComponentType;
  }

  public ComponentType<ChunkStore, FarmComponent> getFarmComponentType() {
    return this.farmComponentType;
  }

  public ComponentType<ChunkStore, WorkplaceInteractablesMapComponent> getWorkplaceInteractablesMapComponentType() {
    return this.workplaceInteractablesMapComponentType;
  }

  public ComponentType<EntityStore, VillagerComponent> getVillagerComponentType() {
    return this.villagerComponentType;
  }

}
