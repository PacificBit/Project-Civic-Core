package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general;

import java.util.ArrayDeque;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderClearSnapshotGroupPositions;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;

public class ClearSnapshotGroupPositions extends ActionBase {
  private String groupName;
  private String conditionalGroup;

  public ClearSnapshotGroupPositions(BuilderClearSnapshotGroupPositions builderActionBase, BuilderSupport support) {
    super(builderActionBase);
    this.groupName = builderActionBase.getGroupName(support);
    this.conditionalGroup = builderActionBase.getConditionalGroup(support);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    super.execute(ref, role, sensorInfo, deltaTime, store);

    VillagerComponent villager = store.getComponent(ref, VillagerComponent.getComponentType());
    if (villager == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[ClearSnapshotGroupPositions] Could not fetch the villager component");
      return false;
    }

    VillagerWorkerComponent workerData = villager.getWorkerData();
    if (workerData == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[ClearSnapshotGroupPositions] Could not fetch the worker's data");
      return false;
    }

    WorkplaceAreaSnapshot snapshot = workerData.getWorkareaSnapshot();
    if (snapshot == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[ClearSnapshotGroupPositions] Could not fetch the work area snapshot");
      return false;
    }

    if (this.conditionalGroup != null && !this.conditionalGroup.isEmpty()) {
      ArrayDeque<Vector3i> conditionalGroup = snapshot.getBlockTypePosition().get(this.conditionalGroup);
      if (conditionalGroup != null && conditionalGroup.size() != 0) {
        return false;
      }
    }

    ArrayDeque<Vector3i> coords = snapshot.getBlockTypePosition().get(this.groupName);
    if (coords != null) {
      coords.clear();
    }

    return true;
  }
}
