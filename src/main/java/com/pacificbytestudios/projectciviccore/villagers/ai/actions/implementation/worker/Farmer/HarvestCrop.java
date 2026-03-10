package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.Farmer;

import com.hypixel.hytale.builtin.adventure.farming.FarmingUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerWorkerComponent;
import com.pacificbytestudios.projectciviccore.villagers.components.workplace.WorkplaceAreaSnapshot;
import com.pacificbytestudios.projectciviccore.villagers.core.work.scan.FarmerScan;

public class HarvestCrop extends ActionBase {

  public HarvestCrop(BuilderActionBase builderActionBase) {
    super(builderActionBase);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    VillagerComponent villager = store.getComponent(ref, VillagerComponent.getComponentType());
    if (villager == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[HarvestCrop] Could not fetch the villager component");
      return false;
    }

    VillagerWorkerComponent workerData = villager.getWorkerData();
    if (workerData == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[HarvestCrop] Could not fetch the worker's data");
      return false;
    }

    WorkplaceAreaSnapshot snapshot = workerData.getWorkareaSnapshot();
    if (snapshot == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[HarvestCrop] Could not fetch the work area snapshot");
      return false;
    }

    World world = store.getExternalData().getWorld();

    Vector3i cropCoord = snapshot.getBlockTypePosition().get(FarmerScan.HARVESTABLE_CROP).pollFirst();

    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(
        cropCoord.x,
        cropCoord.z));
    if (chunk == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[HarvestCrop] Could not fetch the chunk");
      return true;
    }
    BlockType cropBlockType = chunk.getBlockType(cropCoord);
    int rotation = getRotationIndex(world, cropCoord.x, cropCoord.y, cropCoord.z);

    return FarmingUtil.harvest(world, store, ref, cropBlockType, rotation, cropCoord);
  }

  private int getRotationIndex(World world, int x, int y, int z) {
    Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
    Ref<ChunkStore> sectionRef = world.getChunkStore().getChunkSectionReference(
        ChunkUtil.chunkCoordinate(x),
        ChunkUtil.chunkCoordinate(y),
        ChunkUtil.chunkCoordinate(z));
    if (sectionRef == null || !sectionRef.isValid()) {
      return 0;
    }

    BlockSection section = (BlockSection) chunkStore.getComponent(
        sectionRef,
        BlockSection.getComponentType());
    if (section == null) {
      return 0;
    }

    return section.getRotationIndex(x, y, z);
  }

}
