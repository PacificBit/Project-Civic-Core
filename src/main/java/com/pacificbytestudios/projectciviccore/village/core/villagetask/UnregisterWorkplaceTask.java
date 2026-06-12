package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;

@SerializableClass
public class UnregisterWorkplaceTask extends AVillageTask {
  public static final BuilderCodec<UnregisterWorkplaceTask> CODEC = UnregisterWorkplaceTaskCodec.CODEC;

  UUID workplace;

  public UnregisterWorkplaceTask() {
  }

  public UnregisterWorkplaceTask(UUID workplace) {
    this.workplace = workplace;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vec3i heartPos,
      VillageHeartComponent heart) {
    Logger.logInfo("Unregistering workplace with id: " + this.workplace);

    WorkplaceComponent workplace = heart.getWorkplaceFromId(this.workplace);
    Set<UUID> affectedWorkers = new HashSet<>();
    if (workplace != null) {
      affectedWorkers.addAll(workplace.getWorkers());
      if (workplace.getBossId() != null) {
        affectedWorkers.add(workplace.getBossId());
      }
    }

    VillageTopologyComponent topology = heart.getComponent(
        VillageTopologyComponent.getComponentType());
    if (topology == null) {
      Logger.logWarning(
          "Could not unregister workplace topology for " + this.workplace
              + " because the village topology component is missing");
    } else {
      topology.unregisterStructure(this.workplace);
    }

    heart.unregisterWorkplace(this.workplace);

    if (affectedWorkers.isEmpty()) {
      return;
    }

    Map<UUID, VillagerComponent> affectedVillagerSnapshots = VillageVillagerEntitySync.snapshotVillagers(
        heart,
        affectedWorkers);
    if (affectedVillagerSnapshots.isEmpty()) {
      return;
    }

    World world = chunk.getWorld();
    world.execute(() -> VillageVillagerEntitySync.persistVillagers(world, affectedVillagerSnapshots));
  }
}
