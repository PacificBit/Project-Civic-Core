package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.miner;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.Mineshaft;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineshaftLevel;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.utils.Logger;

public class BuildNewMineshaftLevelAction extends VillagerActionBase {

  public BuildNewMineshaftLevelAction(BuilderActionBase builderActionBase) {
    super(builderActionBase);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      ExecutionSupport executionSupport,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!this.canExecute(ref, executionSupport, sensorInfo, deltaTime, store)) {
      return false;
    }
    super.execute(ref, executionSupport, sensorInfo, deltaTime, store);

    MineComponent mineComponent = this.getComponentFromWorkplace(store, ref, MineComponent.getComponentType());
    WorkplaceComponent workplace = this.getWorkplaceComponent(ref, store);
    if (mineComponent == null || workplace == null) {
      return false;
    }

    Mineshaft mineshaft = mineComponent.getMineshaft();
    MineshaftLevel level = mineshaft.createNewMineshaftLevel();

    World world = this.getWorld(store);
    TrackedPrefabPlacementComponent newSnapshot = PrefabManager.spawnTrackedPrefab(
        world,
        StructureConfig.Mine.MineshaftLevelConf.PREFAB_JSON,
        Vec3i.from(level.getAnchorPoint()),
        mineshaft.getRotation(),
        true);
    if (newSnapshot == null) {
      Logger.logWarning("could not build new mineshaft level");
      return false;
    }

    level.setTrackedPrefab(newSnapshot);
    Utils.markNeedSaving(world, workplace.getBenchPos());
    return true;
  }

}
