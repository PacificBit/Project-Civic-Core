package com.pacificbytestudios.projectciviccore.village.systems.workplace;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.codecs.Interactable;
import com.pacificbytestudios.projectciviccore.registry.components.VillageFoodComponent;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.Utils.BlockContext;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceInteractibles;

public class TavernFoodRemovalSystem extends RefSystem<ChunkStore> {

  @Override
  public void onEntityAdded(
      Ref<ChunkStore> ref,
      AddReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
  }

  @Override
  public void onEntityRemove(
      Ref<ChunkStore> ref,
      RemoveReason reason,
      Store<ChunkStore> store,
      CommandBuffer<ChunkStore> commandBuffer) {
    if (!reason.equals(RemoveReason.REMOVE)) {
      return;
    }

    VillageFoodComponent foodComponent = commandBuffer.getComponent(ref, VillageFoodComponent.getComponentType());
    if (foodComponent == null || !foodComponent.hasValidTavernRef()) {
      return;
    }

    Ref<ChunkStore> tavernRef = foodComponent.getTavernRef().getRef();
    if (tavernRef == null || !tavernRef.isValid()) {
      return;
    }

    BlockContext blockContext = Utils.getWorldBlockContext(ref, commandBuffer);
    if (blockContext == null) {
      return;
    }

    WorkplaceInteractablesMapComponent interactables = store.getComponent(
        tavernRef,
        WorkplaceInteractablesMapComponent.getComponentType());
    if (interactables == null) {
      return;
    }

    Interactable interactable = interactables.getInteractableDependingOn(
        WorkplaceInteractibles.EATING_SPOT,
        blockContext.position());
    if (interactable == null || !interactable.isInvalidateOnUse()) {
      return;
    }

    commandBuffer.getExternalData().getWorld().execute(() -> {
      interactable.invalidate();
      store.putComponent(tavernRef, WorkplaceInteractablesMapComponent.getComponentType(), interactables);
      Utils.markNeedSaving(blockContext.chunk().getWorld(), foodComponent.getTavernRef().getPosition());
    });
  }

  @Override
  public Query<ChunkStore> getQuery() {
    return Query.and(VillageFoodComponent.getComponentType());
  }
}
