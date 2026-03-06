package com.pacificbytestudios.projectciviccore;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.pacificbytestudios.projectciviccore.codecs.ClaimedAreaComponent;
import com.pacificbytestudios.projectciviccore.village.commands.VillageCommands;
import com.pacificbytestudios.projectciviccore.village.components.beds.VillageBedComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageSpawnComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.WorkplaceSpawnComponent;
import com.pacificbytestudios.projectciviccore.village.systems.heart.VillageHeartLoadUnloadSystem;
import com.pacificbytestudios.projectciviccore.village.systems.heart.VillageHeartPlacementSystem;
import com.pacificbytestudios.projectciviccore.village.systems.villagetask.VillageTaskQueueSystem;
import com.pacificbytestudios.projectciviccore.village.systems.workplace.WorkplaceSpawnSystem;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.BuilderBedSensor;
import com.pacificbytestudios.projectciviccore.villagers.ai.sensors.builders.BuilderWatchVillageStateSensor;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;

public final class ProjectCivicCore extends JavaPlugin {
  public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  private static ProjectCivicCore INSTANCE;
  private ComponentType<ChunkStore, VillageHeartComponent> villageCoreComponentType;
  private ComponentType<ChunkStore, VillageSpawnComponent> villageSpawnComponentType;
  private ComponentType<ChunkStore, VillageBedComponent> villageBedComponentType;
  private ComponentType<ChunkStore, WorkplaceComponent> workplacePlaceComponentType;
  private ComponentType<ChunkStore, WorkplaceSpawnComponent> workplacePlaceSpawnComponentType;
  private ComponentType<ChunkStore, ClaimedAreaComponent> claimedAreaComponentType;
  private ComponentType<ChunkStore, VillageTaskQueueComponent> villageTaskQueueComponentType;

  private ComponentType<EntityStore, VillagerComponent> villagerComponentType;

  public ProjectCivicCore(JavaPluginInit init) {
    super(init);
    INSTANCE = this;
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

    this.workplacePlaceComponentType = chunkRegistry.registerComponent(
        WorkplaceComponent.class,
        "WorkplaceComponent",
        WorkplaceComponent.CODEC);

    this.workplacePlaceSpawnComponentType = chunkRegistry.registerComponent(
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

    chunkRegistry.registerSystem(new VillageHeartPlacementSystem());
    chunkRegistry.registerSystem(new VillageHeartLoadUnloadSystem());
    chunkRegistry.registerSystem(new WorkplaceSpawnSystem());
    chunkRegistry.registerSystem(new VillageTaskQueueSystem());

    commandRegistry.registerCommand(new VillageCommands());

    this.villagerComponentType = entityRegistry.registerComponent(
        VillagerComponent.class,
        "VillagerComponent",
        VillagerComponent.CODEC);

    npcPlugin.registerCoreComponentType(
        "WatchVillageState",
        BuilderWatchVillageStateSensor::new);

    npcPlugin.registerCoreComponentType(
        "BedSensor",
        BuilderBedSensor::new);
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

  public ComponentType<ChunkStore, WorkplaceComponent> getWorkplacePlaceComponentType() {
    return this.workplacePlaceComponentType;
  }

  public ComponentType<ChunkStore, WorkplaceSpawnComponent> getWorkplacePlaceSpawnComponentType() {
    return this.workplacePlaceSpawnComponentType;
  }

  public ComponentType<ChunkStore, ClaimedAreaComponent> getClaimedAreaComponent() {
    return this.claimedAreaComponentType;
  }

  public ComponentType<ChunkStore, VillageTaskQueueComponent> getVillageTaskQueueComponentType() {
    return this.villageTaskQueueComponentType;
  }

  public ComponentType<EntityStore, VillagerComponent> getVillagerComponentType() {
    return this.villagerComponentType;
  }
}
