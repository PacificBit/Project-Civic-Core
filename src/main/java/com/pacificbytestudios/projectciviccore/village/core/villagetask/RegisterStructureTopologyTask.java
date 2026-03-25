package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.TopologyStructure;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageTopologyStructureType;

public class RegisterStructureTopologyTask extends AVillageTask {
  public static final BuilderCodec<RegisterStructureTopologyTask> CODEC = BuilderCodec
      .builder(RegisterStructureTopologyTask.class, RegisterStructureTopologyTask::new)

      .append(new KeyedCodec<>("StructureId", Codec.UUID_BINARY),
          (obj, value) -> obj.structureId = value,
          obj -> obj.structureId)
      .add()

      .append(new KeyedCodec<>("StructureType", new EnumCodec<StructureType>(StructureType.class)),
          (obj, value) -> obj.structureType = value,
          obj -> obj.structureType)
      .add()

      .append(new KeyedCodec<>("PrefabAnchor", Vector3i.CODEC),
          (obj, value) -> obj.prefabAnchor = value,
          obj -> obj.prefabAnchor)
      .add()

      .append(new KeyedCodec<>("Rotation", Rotation.CODEC, false),
          (obj, value) -> obj.rotation = value,
          obj -> obj.rotation)
      .add()

      .build();

  private UUID structureId;
  private StructureType structureType;
  private Vector3i prefabAnchor;
  private Rotation rotation = Rotation.None;

  public RegisterStructureTopologyTask() {
  }

  public RegisterStructureTopologyTask(
      UUID structureId,
      StructureType structureType,
      Vector3i prefabAnchor,
      Rotation rotation) {
    this.structureId = structureId;
    this.structureType = structureType;
    this.prefabAnchor = prefabAnchor;
    this.rotation = rotation != null ? rotation : Rotation.None;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vector3i heartPos,
      VillageHeartComponent heart) {
    if (this.structureId == null || this.structureType == null || this.prefabAnchor == null) {
      ProjectCivicCore.LOGGER.atWarning().log(
          "[RegisterStructureTopologyTask] Could not register structure topology because the task data is incomplete");
      return;
    }

    VillageTopologyComponent topology = heartRef.getStore().getComponent(
        heartRef,
        VillageTopologyComponent.getComponentType());
    if (topology == null) {
      ProjectCivicCore.LOGGER.atWarning().log(
          "[RegisterStructureTopologyTask] Could not register structure topology for " + this.structureId
              + " because the village topology component is missing");
      return;
    }

    Vector2i structureSize = Utils.getSize2i(this.structureType);
    if (structureSize == null) {
      ProjectCivicCore.LOGGER.atWarning().log(
          "[RegisterStructureTopologyTask] Could not register structure topology for " + this.structureId
              + " because the structure size is missing");
      return;
    }

    TopologyStructure topologyStructure = new TopologyStructure(
        this.prefabAnchor,
        structureSize,
        VillageTopologyStructureType.fromStructureType(this.structureType),
        this.rotation == null ? Rotation.None : this.rotation);
    topologyStructure.setStructureId(this.structureId);
    topologyStructure.setStructureType(this.structureType);
    topologyStructure.setPrefabAnchor(this.prefabAnchor);

    ProjectCivicCore.LOGGER.atInfo().log(
        "[RegisterStructureTopologyTask] Registering topology for '" + this.structureType + "' with id: "
            + this.structureId);
    topology.registerStructure(topologyStructure);
  }
}
