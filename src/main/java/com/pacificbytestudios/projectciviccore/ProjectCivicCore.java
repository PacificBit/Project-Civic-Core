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
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.codecs.VillageTaskQueueCodecs;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.village.commands.VillageCommands;
import com.pacificbytestudios.projectciviccore.village.components.beds.VillageBedComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageSpawnComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerEntryComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceContainerRegistryComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceSpawnComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.WorkplaceVillageStorageRequestComponent;
import com.pacificbytestudios.projectciviccore.village.systems.heart.VillageHeartLoadUnloadSystem;
import com.pacificbytestudios.projectciviccore.village.systems.heart.VillageHeartPlacementSystem;
import com.pacificbytestudios.projectciviccore.village.systems.villagetask.VillageTaskQueueSystem;
import com.pacificbytestudios.projectciviccore.village.systems.workplace.WorkplaceSpawnSystem;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderChangeIntoWorkRoleAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderClearSnapshotGroupPositions;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderEquipItemFromInventoryAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.BuilderScanWorkAreaAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.Farmer.BuilderHarvestCropAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.Farmer.BuilderPlantSeedAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.Farmer.BuilderTillSoilAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.Farmer.BuilderWaterSoilAction;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderBedSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderTimeRangeSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderWatchForJobAssignment;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.general.BuilderWatchVillageStateSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderHasTargetBlockInSnapshotSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderHasValidWorkAreaSnapshotSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderReachNextTargetInSnapshotSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.worker.general.BuilderReachWorkplaceSensor;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.interactions.VillagerOpenInventoryInteraction;
import com.pacificbytestudios.projectciviccore.villagers.systems.EnsureVillagerComponents;
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

    this.getCodecRegistry(Interaction.CODEC).register(
        "VillagerOpenInventoryInteraction",
        VillagerOpenInventoryInteraction.class,
        VillagerOpenInventoryInteraction.CODEC);
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

  public ComponentType<EntityStore, VillagerComponent> getVillagerComponentType() {
    return this.villagerComponentType;
  }
}
