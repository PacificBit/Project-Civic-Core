package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.BuilderScanWorkAreaAction;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.IWorkAreaScanner;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.ScanFactory;

public class ScanWorkAreaAction extends VillagerActionBase {

  public ScanWorkAreaAction(BuilderScanWorkAreaAction builderActionBase) {
    super(builderActionBase);
  }

  public boolean execute(
      Ref<EntityStore> ref,
      ExecutionSupport executionSupport,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    super.execute(ref, executionSupport, sensorInfo, deltaTime, store);

    UUID villagerId = this.getVillagerId(ref, store);
    VillagerComponent villager = this.getVillagerComponent(ref, store);
    VillageHeartComponent village = this.getVillageHeartComponent(ref, store);
    if (villager == null || village == null || villagerId == null) {
      return false;
    }

    IWorkAreaScanner scanner = ScanFactory.newScanner(villager);

    if (scanner == null) {
      return false;
    }

    WorkplaceAreaSnapshot snapshot = scanner.execute(
        store,
        ref,
        villagerId,
        villager);

    villager.getWorkerData().setWorkareaSnapshot(snapshot);

    if (village.getConfig().isDebugActive()) {
      // TODO: draw snapshot BlockTarget
    }

    return snapshot.isCompleted();
  }
}
