package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation;

import java.util.ArrayDeque;
import java.util.function.Supplier;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.BuilderWorkplaceStorageTargetsSnapshotAction;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;

public class WorkplaceStorageTargetsSnapshotAction extends ActionBase {
  public static final String WORKPLACE_STORAGE_KEY = "Worker_Workplace_Storage";

  private ActionType actionType;

  public WorkplaceStorageTargetsSnapshotAction(
      BuilderWorkplaceStorageTargetsSnapshotAction builderActionBase,
      BuilderSupport support) {
    super(builderActionBase);
    this.actionType = builderActionBase.getActionType(support);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!this.canExecute(ref, role, sensorInfo, deltaTime, store)) {
      return false;
    }

    VillagerComponent villager = store.getComponent(ref,
        VillagerComponent.getComponentType());

    VillagerWorkerComponent workerData = villager.getWorkerData();
    if (workerData == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[WorkplaceStorageTargetsSnapshotAction] Could not fetch worker data");
      return false;
    }

    WorkplaceAreaSnapshot snapshot = workerData.getWorkareaSnapshot();
    if (snapshot == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[WorkplaceStorageTargetsSnapshotAction] Could not fetch snapshot data");
      return false;
    }

    WorkplaceComponent workplace = workerData.getWorkplace();
    if (workplace == null) {
      ProjectCivicCore.LOGGER.atSevere()
          .log("[WorkplaceStorageTargetsSnapshotAction] Could not fetch the workplace component");
      return false;
    }

    System.out.println("Executing workplace storage action: " + this.actionType);

    if (this.actionType.equals(ActionType.ADD)) {
      ArrayDeque<Vector3i> storagePosition = new ArrayDeque<>();
      storagePosition.addAll(workplace.getStorage().getAllContainerPositions());
      snapshot.getBlockTypePosition().put(WORKPLACE_STORAGE_KEY, storagePosition);
    } else if (snapshot.getBlockTypePosition().get(WORKPLACE_STORAGE_KEY) != null) {
      snapshot.getBlockTypePosition().remove(WORKPLACE_STORAGE_KEY);
    }

    System.out.println("SNAPSHOT RESULT: ");
    System.out.println(snapshot);
    return true;
  }

  public enum ActionType implements Supplier<String> {
    ADD("add"),
    REMOVE("remove");

    private final String type;

    ActionType(String type) {
      this.type = type;
    }

    @Override
    public String get() {
      return this.type;
    }
  }

}
