package com.pacificbytestudios.projectciviccore.villagers.systems;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.Dirty;
import com.hypixel.hytale.server.core.event.events.ecs.InventoryChangeEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;

public final class VillagerInventoryPersistenceSystem extends EntityEventSystem<EntityStore, InventoryChangeEvent> {

  public VillagerInventoryPersistenceSystem() {
    super(InventoryChangeEvent.class);
  }

  @Override
  public Query<EntityStore> getQuery() {
    return Query.and(
        NPCEntity.getComponentType(),
        VillagerComponent.getComponentType(),
        TransformComponent.getComponentType());
  }

  @Override
  public void handle(
      int index,
      @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer,
      @Nonnull InventoryChangeEvent event) {
    Dirty dirty = archetypeChunk.getComponent(index, Dirty.getComponentType());

    if (dirty != null) {
      dirty.markDirty();
    }
  }
}
