package com.pacificbytestudios.projectciviccore.villagers.systems;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnVillagerTask;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;

public class EnsureVillagerComponents extends HolderSystem<EntityStore> {

  @Override
  public Query<EntityStore> getQuery() {
    return VillagerComponent.getComponentType();
  }

  @Override
  public void onEntityAdd(
      Holder<EntityStore> holder,
      AddReason reason,
      Store<EntityStore> store) {
    holder.ensureComponent(Interactable.getComponentType());
    holder.putComponent(Interactions.getComponentType(),
        SpawnVillagerTask.buildInteractions());
  }

  @Override
  public void onEntityRemoved(
      Holder<EntityStore> holder,
      RemoveReason removeReason,
      Store<EntityStore> store) {
  }
}
