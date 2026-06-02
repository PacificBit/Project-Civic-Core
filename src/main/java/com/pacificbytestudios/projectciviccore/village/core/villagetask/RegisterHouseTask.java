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
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.enums.VillageTopologyStructureType;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class RegisterHouseTask extends AVillageTask {
  public static final BuilderCodec<RegisterHouseTask> CODEC = BuilderCodec
      .builder(RegisterHouseTask.class, RegisterHouseTask::new)

      .append(new KeyedCodec<>("House", HouseComponent.CODEC),
          (obj, value) -> obj.house = value,
          obj -> obj.house)
      .add()

      .build();

  private HouseComponent house;

  public RegisterHouseTask() {
  }

  public RegisterHouseTask(HouseComponent house) {
    this.house = house;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vec3i heartPos,
      VillageHeartComponent heart) {
    if (this.house == null) {
      Logger.logWarning(
          "Could not register house because the house component is missing");
      return;
    }

    Logger.logInfo("Registering house with id: " + this.house.getUuid());
    heart.registerHouse(this.house);

    VillageTopologyComponent topology = heartRef.getStore().getComponent(
        heartRef,
        VillageTopologyComponent.getComponentType());
    if (topology == null) {
      Logger.logWarning(
          "Could not register house topology because the village topology component is missing");
      return;
    }

    TopologyStructure houseTopology = buildHouseTopology(this.house);
    if (houseTopology == null) {
      return;
    }

    topology.registerStructure(houseTopology);
  }

  private static TopologyStructure buildHouseTopology(HouseComponent house) {
    if (house == null) {
      Logger.logWarning(
          "Could not register house topology because the house component is missing");
      return null;
    }

    if (house.getUuid() == null) {
      Logger.logWarning(
          "Could not register house topology because the house UUID is missing");
      return null;
    }

    Vec3i prefabAnchor = house.getPrefabAnchor();
    if (prefabAnchor == null) {
      Logger.logWarning(
          "Could not register house topology for " + house.getUuid()
              + " because the prefab anchor is missing");
      return null;
    }

    if (house.getHouseRef() == null) {
      Logger.logWarning(
          "Could not register house topology for " + house.getUuid()
              + " because the house ref is missing");
      return null;
    }

    Ref<ChunkStore> houseRef = house.getHouseRef().getRef();
    if (houseRef == null || !houseRef.isValid()) {
      Logger.logWarning(
          "Could not register house topology for " + house.getUuid()
              + " because the house ref is invalid");
      return null;
    }

    TrackedPrefabPlacementComponent trackedPlacement = houseRef.getStore().getComponent(
        houseRef,
        TrackedPrefabPlacementComponent.getComponentType());
    if (trackedPlacement == null) {
      Logger.logWarning(
          "Could not register house topology for " + house.getUuid()
              + " because the tracked prefab placement is missing");
      return null;
    }

    Vec2i structureSize = Utils.getSize2i(StructureType.HOUSE);
    if (structureSize == null) {
      Logger.logWarning(
          "Could not register house topology for " + house.getUuid()
              + " because the structure size is missing");
      return null;
    }

    return buildTopologyStructure(house, structureSize, trackedPlacement.getYaw());
  }

  private static TopologyStructure buildTopologyStructure(
      HouseComponent house,
      Vec2i structureSize,
      Rotation orientation) {
    if (house == null || structureSize == null) {
      return null;
    }

    Vec3i prefabAnchor = house.getPrefabAnchor();
    if (house.getUuid() == null || prefabAnchor == null) {
      return null;
    }

    TopologyStructure topologyStructure = new TopologyStructure(
        prefabAnchor,
        structureSize,
        VillageTopologyStructureType.HOUSE,
        orientation == null ? Rotation.None : orientation);
    topologyStructure.setStructureId(house.getUuid());
    topologyStructure.setStructureType(StructureType.HOUSE);
    topologyStructure.setPrefabAnchor(prefabAnchor);
    return topologyStructure;
  }
}
