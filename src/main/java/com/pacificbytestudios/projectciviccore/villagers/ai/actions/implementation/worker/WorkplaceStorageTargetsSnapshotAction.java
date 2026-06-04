package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker;

import java.util.ArrayDeque;
import java.util.function.Supplier;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.worker.BuilderWorkplaceStorageTargetsSnapshotAction;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;

public class WorkplaceStorageTargetsSnapshotAction extends VillagerActionBase {
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

    WorkplaceComponent workplace = this.getWorkplaceComponent(ref, store);
    WorkplaceAreaSnapshot snapshot = this.getAreaSnapshot(ref, store);
    if (workplace == null || snapshot == null) {
      return false;
    }

    if (this.actionType.equals(ActionType.ADD)) {
      ArrayDeque<Vec3i> storagePosition = new ArrayDeque<>();
      for (Vec3f accessPosition : workplace.getStorage().getAllContainerAccessPositions()) {
        storagePosition.add(accessPosition.to3i());
      }
      snapshot.getBlockTypePosition().put(WORKPLACE_STORAGE_KEY, storagePosition);
    } else if (snapshot.getBlockTypePosition().get(WORKPLACE_STORAGE_KEY) != null) {
      snapshot.getBlockTypePosition().remove(WORKPLACE_STORAGE_KEY);
    }
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
