package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general;

import java.util.ArrayDeque;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderClearSnapshotGroupPositions;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;

public class ClearSnapshotGroupPositions extends VillagerActionBase {
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

    WorkplaceAreaSnapshot snapshot = this.getAreaSnapshot(ref, store);
    if (snapshot == null) {
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
