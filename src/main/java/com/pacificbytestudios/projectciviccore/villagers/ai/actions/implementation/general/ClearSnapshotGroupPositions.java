package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general;

import java.util.ArrayDeque;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
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
      ExecutionSupport executionSupport,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    super.execute(ref, executionSupport, sensorInfo, deltaTime, store);

    WorkplaceAreaSnapshot snapshot = this.getAreaSnapshot(ref, store);
    if (snapshot == null) {
      return false;
    }

    if (this.conditionalGroup != null && !this.conditionalGroup.isEmpty()) {
      ArrayDeque<Vec3i> conditionalGroup = snapshot.getBlockTypePosition().get(this.conditionalGroup);
      if (conditionalGroup != null && conditionalGroup.size() != 0) {
        return false;
      }
    }

    ArrayDeque<Vec3i> coords = snapshot.getBlockTypePosition().get(this.groupName);
    if (coords != null) {
      coords.clear();
    }

    return true;
  }
}
