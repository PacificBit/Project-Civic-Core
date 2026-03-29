package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.miner;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.prefab.TrackedPrefabPlacementComponent;
import com.pacificbytestudios.projectciviccore.utils.PrefabManager;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.Mineshaft;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineshaftLevel;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;

public class BuildNewMineshaftLevelAction extends VillagerActionBase {

  public BuildNewMineshaftLevelAction(BuilderActionBase builderActionBase) {
    super(builderActionBase);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!this.canExecute(ref, role, sensorInfo, deltaTime, store)) {
      return false;
    }
    super.execute(ref, role, sensorInfo, deltaTime, store);

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
        level.getAnchorPoint().clone(),
        mineshaft.getRotation(),
        true);
    if (newSnapshot == null) {
      ProjectCivicCore.LOGGER.atSevere().log("[BuildNewMineshaftLevelAction] could not build new mineshaft level");
      return false;
    }

    level.setTrackedPrefab(newSnapshot);
    Utils.markNeedSaving(world, workplace.getBenchPos());
    return true;
  }

}
