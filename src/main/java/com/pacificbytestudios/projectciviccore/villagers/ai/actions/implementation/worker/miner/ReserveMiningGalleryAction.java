package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.miner;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class ReserveMiningGalleryAction extends VillagerActionBase {

  public ReserveMiningGalleryAction(BuilderActionBase builderActionBase) {
    super(builderActionBase);
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

    UUID galleryId = mineComponent.getFirstAvailableGalleryId();
    if (galleryId == null) {
      Logger.logInfo("Could not find a valid gallery to mine");
      return false;
    }

    return mineComponent.registerAsGalleryWorker(villagerId, galleryId);
  }

}
