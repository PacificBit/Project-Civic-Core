package com.pacificbytestudios.projectciviccore.villagers.core.work.scan;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;

public interface IWorkAreaScanner {
  public static final String TERRAIN_BLOCK_TO_FILL = "Terrain_Block_To_Fill";
  public static final String BREAKABLE_BLOCK = "Breakable_Block";

  public WorkplaceAreaSnapshot execute(
      Store<EntityStore> store,
      Ref<EntityStore> villagerRef,
      UUID uuid,
      VillagerComponent villager);
}
