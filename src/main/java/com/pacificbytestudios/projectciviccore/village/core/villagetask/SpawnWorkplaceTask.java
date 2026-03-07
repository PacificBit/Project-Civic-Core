package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.utils.SpawnFunction;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceType;

public class SpawnWorkplaceTask implements IVillageTask {

  WorkplaceType workplaceType;
  Vector3i position;

  SpawnWorkplaceTask(WorkplaceType workplaceType, Vector3i position) {
    this.workplaceType = workplaceType;
    this.position = position;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vector3i heartPos,
      VillageHeartComponent heart) {
    SpawnFunction spawnFunction = Utils.getSpawnFunction(this.workplaceType);
    spawnFunction.apply(chunk, this.position);
  }

}
