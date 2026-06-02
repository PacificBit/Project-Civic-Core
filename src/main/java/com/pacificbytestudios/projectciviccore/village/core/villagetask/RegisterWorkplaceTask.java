package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D.Vec2i;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageTopologyStructureType;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class RegisterWorkplaceTask extends AVillageTask {
  public static final BuilderCodec<RegisterWorkplaceTask> CODEC = BuilderCodec
      .builder(RegisterWorkplaceTask.class, RegisterWorkplaceTask::new)

      .append(new KeyedCodec<>("Workplace", WorkplaceComponent.CODEC),
          (obj, value) -> obj.workplace = value,
          (obj) -> obj.workplace)
      .add()

      .build();

  private WorkplaceComponent workplace;

  public RegisterWorkplaceTask() {
  }

  public RegisterWorkplaceTask(WorkplaceComponent workplace) {
    this.workplace = workplace;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vec3i heartPos,
      VillageHeartComponent heart) {
    Logger.logInfo("Registering workplace '" + this.workplace.getWorkplaceType() + "' with id: "
            + this.workplace.getUuid());
    heart.registerWorkplace(this.workplace);

    VillageTopologyComponent topology = heartRef.getStore().getComponent(
        heartRef,
        VillageTopologyComponent.getComponentType());

    TopologyStructure workplaceTopology = buildWorkplaceTopology(this.workplace);
    if (workplaceTopology == null) {
      return;
    }

    topology.registerStructure(workplaceTopology);
  }

  private static TopologyStructure buildWorkplaceTopology(WorkplaceComponent workplace) {
    StructureType workplaceType = workplace.getWorkplaceType();

    Ref<ChunkStore> workplaceRef = workplace.getWorkplaceRef().getRef();
    if (workplaceRef == null || !workplaceRef.isValid()) {
      Logger.logWarning(
          "Could not register workplace topology for " + workplace.getUuid()
              + " because the workplace ref is invalid");
      return null;
    }

    TrackedPrefabPlacementComponent trackedPlacement = workplaceRef.getStore().getComponent(
        workplaceRef,
        TrackedPrefabPlacementComponent.getComponentType());
    if (trackedPlacement == null) {
      Logger.logWarning(
          "Could not register workplace topology for " + workplace.getUuid()
              + " because the tracked prefab placement is missing");
      return null;
    }

    Vec2i structureSize = Utils.getSize2i(workplaceType);
    return buildTopologyStructure(workplace, structureSize, trackedPlacement.getYaw());
  }

  private static TopologyStructure buildTopologyStructure(
      WorkplaceComponent workplace,
      Vec2i structureSize,
      Rotation orientation) {
    if (workplace == null || structureSize == null) {
      return null;
    }

    StructureType workplaceType = workplace.getWorkplaceType();
    Vec3i prefabAnchor = workplace.getPrefabAnchor();
    if (workplace.getUuid() == null || workplaceType == null || prefabAnchor == null) {
      return null;
    }

    TopologyStructure topologyStructure = new TopologyStructure(
        prefabAnchor,
        structureSize,
        VillageTopologyStructureType.fromStructureType(workplaceType),
        orientation == null ? Rotation.None : orientation);
    topologyStructure.setStructureId(workplace.getUuid());
    topologyStructure.setStructureType(workplaceType);
    topologyStructure.setPrefabAnchor(workplace.getPrefabAnchor());
    return topologyStructure;
  }
}
