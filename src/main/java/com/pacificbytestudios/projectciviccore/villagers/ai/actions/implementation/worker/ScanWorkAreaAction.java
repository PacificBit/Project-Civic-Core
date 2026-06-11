package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.BuilderScanWorkAreaAction;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.IWorkAreaScanner;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.ScanFactory;

public class ScanWorkAreaAction extends ActionBase {

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

    VillagerComponent villager = store.getComponent(ref, VillagerComponent.getComponentType());
    assert villager != null : "Could not locate the villager component";

    UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());

    assert uuidComponent != null : "Could not get the villager uuid";

    IWorkAreaScanner scanner = ScanFactory.newScanner(villager);

    if (scanner == null) {
      return false;
    }

    WorkplaceAreaSnapshot snapshot = scanner.execute(
        store,
        ref,
        uuidComponent.getUuid(),
        villager);

    villager.getWorkerData().setWorkareaSnapshot(snapshot);

    return snapshot.isCompleted();
  }
}
