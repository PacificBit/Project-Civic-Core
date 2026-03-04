package com.pacificbytestudios.projectciviccore;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.villageheart.commands.VillageCommands;
import com.pacificbytestudios.projectciviccore.villageheart.components.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.villageheart.components.VillageSpawnComponent;
import com.pacificbytestudios.projectciviccore.villageheart.systems.VillageHeartPlacementSystem;

public final class ProjectCivicCore extends JavaPlugin {
  public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  private static ProjectCivicCore INSTANCE;
  private ComponentType<ChunkStore, VillageHeartComponent> villageCoreComponentType;
  private ComponentType<ChunkStore, VillageSpawnComponent> villageSpawnComponentType;

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

    this.villageSpawnComponentType = this.getChunkStoreRegistry().registerComponent(
        VillageSpawnComponent.class,
        "VillageSpawnConfig",
        VillageSpawnComponent.CODEC);

    this.villageCoreComponentType = this.getChunkStoreRegistry().registerComponent(
        VillageHeartComponent.class,
        "VillageHeartComponent",
        VillageHeartComponent.CODEC);

    this.getChunkStoreRegistry().registerSystem(new VillageHeartPlacementSystem());

    this.getCommandRegistry().registerCommand(new VillageCommands());
  }

  public ComponentType<ChunkStore, VillageSpawnComponent> getVillageSpawnComponentType() {
    return this.villageSpawnComponentType;
  }

  public ComponentType<ChunkStore, VillageHeartComponent> getVillageCoreComponentType() {
    return this.villageCoreComponentType;
  }

}
