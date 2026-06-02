package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.UUID;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class UnregisterHouseTask extends AVillageTask {
  public static final BuilderCodec<UnregisterHouseTask> CODEC = BuilderCodec
      .builder(UnregisterHouseTask.class, UnregisterHouseTask::new)

      .append(new KeyedCodec<>("House", Codec.UUID_BINARY),
          (obj, value) -> obj.house = value,
          obj -> obj.house)
      .add()

      .build();

  private UUID house;

  public UnregisterHouseTask() {
  }

  public UnregisterHouseTask(UUID house) {
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
          "Could not unregister house because the house UUID is missing");
      return;
    }

    Logger.logInfo("Unregistering house with id: " + this.house);

    VillageTopologyComponent topology = heartRef.getStore().getComponent(
        heartRef,
        VillageTopologyComponent.getComponentType());
    if (topology == null) {
      Logger.logWarning(
          "Could not unregister house topology for " + this.house
              + " because the village topology component is missing");
    } else {
      topology.unregisterStructure(this.house);
    }

    heart.unregisterHouse(this.house);
  }
}
