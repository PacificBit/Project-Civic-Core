package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.List;
import java.util.UUID;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class TeleportVillagerToTownHeartTask extends AVillageTask {
  public static final BuilderCodec<TeleportVillagerToTownHeartTask> CODEC = BuilderCodec
      .builder(TeleportVillagerToTownHeartTask.class, TeleportVillagerToTownHeartTask::new)

      .append(new KeyedCodec<>("VillagerId", Codec.UUID_BINARY),
          (obj, value) -> obj.villagerId = value,
          obj -> obj.villagerId)
      .add()

      .build();

  private UUID villagerId;

  public TeleportVillagerToTownHeartTask() {
  }

  public TeleportVillagerToTownHeartTask(UUID villagerId) {
    this.villagerId = villagerId;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vec3i heartPos,
      VillageHeartComponent heart) {
    if (this.villagerId == null || heart == null || heart.getVillager(this.villagerId) == null) {
      return;
    }

    Ref<EntityStore> villagerRef = chunk.getWorld().getEntityRef(this.villagerId);
    if (villagerRef == null || !villagerRef.isValid()) {
      return;
    }

    Store<EntityStore> entityStore = chunk.getWorld().getEntityStore().getStore();
    TransformComponent transform = entityStore.getComponent(villagerRef, TransformComponent.getComponentType());
    if (transform == null) {
      return;
    }

    entityStore.addComponent(
        villagerRef,
        Teleport.getComponentType(),
        Teleport.createExact(resolveTargetPosition(heartPos), transform.getRotation().clone()));
    this.callOnSuccess(this.villagerId.toString());

    Logger.logInfo("Teleporting villager %s to town heart"
        .formatted(this.villagerId));
  }

  private static Vec3d resolveTargetPosition(Vec3i heartPos) {
    List<Vec3i> spawningOffsets = StructureConfig.TownHeart.getVillagerSpawningOffsets();
    if (spawningOffsets == null || spawningOffsets.isEmpty()) {
      return Vec3d.from(heartPos);
    }

    Vec3i target = Vec3i.from(heartPos).add(spawningOffsets.getFirst());

    return Vec3d.from(target);
  }
}
