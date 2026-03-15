package com.pacificbytestudios.projectciviccore.villagers.systems;

import com.hypixel.hytale.builtin.mounts.BlockMountAPI;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.events.SitOnChairEvent;

public final class SitOnChairSystem extends EntityEventSystem<EntityStore, SitOnChairEvent> {

  public SitOnChairSystem() {
    super(SitOnChairEvent.class);
  }

  @Override
  public Query<EntityStore> getQuery() {
    return Query.and(
        NPCEntity.getComponentType(),
        VillagerComponent.getComponentType());
  }

  @Override
  public void handle(
      int index,
      ArchetypeChunk<EntityStore> archetypeChunk,
      Store<EntityStore> store,
      CommandBuffer<EntityStore> commandBuffer,
      SitOnChairEvent event) {
    Ref<EntityStore> villagerRef = archetypeChunk.getReferenceTo(index);

    event.setResult(BlockMountAPI.mountOnBlock(
        villagerRef,
        commandBuffer,
        event.getTargetBlock(),
        event.getInteractPos()));
  }
}
