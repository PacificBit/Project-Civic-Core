package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general;

import java.util.List;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.farm.FarmComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.mine.MineComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.sawmill.SawmillComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.tavern.TavernComponent;
import com.pacificbytestudios.projectciviccore.village.core.interfaces.IWorkplaceMissingResource;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderSetMissingResources;
import com.pacificbytestudios.projectciviccore.villagers.core.inventory.InventoryItemCriteria;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;

public class SetMissingResources extends VillagerActionBase {
  private List<InventoryItemCriteria> itemCriterias;
  private boolean resetMissingResources;

  public SetMissingResources(
      BuilderSetMissingResources builderActionBase,
      BuilderSupport support) {
    super(builderActionBase);
    this.itemCriterias = builderActionBase.getItemCriteria(support);
    this.resetMissingResources = builderActionBase.getResetMissingResourcesFlag();
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!super.canExecute(ref, role, sensorInfo, deltaTime, store)) {
      return false;
    }
    super.execute(ref, role, sensorInfo, deltaTime, store);

    WorkplaceComponent workplace = this.getWorkplaceComponent(ref, store);
    Ref<ChunkStore> workplaceRef = this.getWorkplaceRef(ref, store);
    if (workplaceRef == null || workplace == null) {
      return false;
    }

    Store<ChunkStore> chunkStore = workplaceRef.getStore();
    IWorkplaceMissingResource missingResourceComponent = require(getProperComponent(
        chunkStore,
        workplaceRef,
        workplace.getWorkplaceType()), "missing workplace resource component");

    if (missingResourceComponent == null) {
      return false;
    }

    World world = workplaceRef.getStore().getExternalData().getWorld();
    Vector3i workplacePosition = workplace.getBenchPos();

    if (this.resetMissingResources) {
      missingResourceComponent.resetMissingResources();
    }

    if (this.itemCriterias.isEmpty()) {
      Utils.markNeedSaving(world, workplacePosition);
      return true;
    }

    this.itemCriterias.stream()
        .map(InventoryItemCriteria::toVillageRequestCriterion)
        .forEach(missingResourceComponent::addMissingResource);

    Utils.markNeedSaving(world, workplacePosition);
    return true;
  }

  private IWorkplaceMissingResource getProperComponent(
      Store<ChunkStore> chunkStore,
      Ref<ChunkStore> ref,
      StructureType workplaceType) {
    return switch (workplaceType) {
      case TAVERN -> chunkStore.getComponent(ref, TavernComponent.getComponentType());
      case FARM -> chunkStore.getComponent(ref, FarmComponent.getComponentType());
      case SAWMILL -> chunkStore.getComponent(ref, SawmillComponent.getComponentType());
      case MINE -> chunkStore.getComponent(ref, MineComponent.getComponentType());
      default -> throw new IllegalArgumentException("Workplace type '" + workplaceType + "' not supported!");
    };
  }
}
