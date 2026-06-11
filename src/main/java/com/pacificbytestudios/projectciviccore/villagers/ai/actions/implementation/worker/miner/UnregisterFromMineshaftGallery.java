package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.miner;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineComponent;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.miner.BuilderUnregisterFromMineshaftGallery;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class UnregisterFromMineshaftGallery extends VillagerActionBase {
  private boolean markAsCompleted;

  public UnregisterFromMineshaftGallery(BuilderUnregisterFromMineshaftGallery builderActionBase,
      BuilderSupport support) {
    super(builderActionBase);
    this.markAsCompleted = builderActionBase.getMarkAsCompleted(support);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      ExecutionSupport executionSupport,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!this.canExecute(ref, executionSupport, sensorInfo, deltaTime, store)) {
      return false;
    }
    super.execute(ref, executionSupport, sensorInfo, deltaTime, store);

    MineComponent mineComponent = this.getComponentFromWorkplace(store, ref, MineComponent.getComponentType());
    UUID villagerId = this.getVillagerId(ref, store);
    if (mineComponent == null || villagerId == null) {
      return false;
    }

    UUID galleryId = mineComponent.unregisterFromGallery(villagerId);
    boolean success = galleryId != null;

    if (!success) {
      Logger.logWarning("Could not unregister gallery");
      return false;
    }

    if (this.markAsCompleted) {
      success = mineComponent.markGalleryAsCompleted(galleryId);
    }

    if (!success) {
      Logger.logWarning("Could not unregister gallery or mark as completed");
    }

    return success;
  }

}
