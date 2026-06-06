package com.pacificbytestudios.projectciviccore.village.systems.workplace;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.codecs.BlockContext;
import com.pacificbytestudios.projectciviccore.codecs.structureconfig.Interactable;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.registry.components.VillageFoodComponent;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.village.enums.InteractablesType;

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
        InteractablesType.EATING_SPOT,
        Vec3f.from(blockContext.getPosition()));
    if (interactable == null || !interactable.isInvalidateOnUse()) {
      return;
    }

    commandBuffer.getExternalData().getWorld().execute(() -> {
      interactable.invalidate();
      store.putComponent(tavernRef, WorkplaceInteractablesMapComponent.getComponentType(), interactables);
      Utils.markNeedSaving(blockContext.getWorld(), foodComponent.getTavernRef().getPosition());
    });
  }

  @Override
  public Query<ChunkStore> getQuery() {
    return Query.and(VillageFoodComponent.getComponentType());
  }
}
