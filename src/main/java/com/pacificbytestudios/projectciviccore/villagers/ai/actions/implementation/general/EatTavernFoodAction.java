package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.general;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.codecs.Interactable;
import com.pacificbytestudios.projectciviccore.registry.components.VillageFoodComponent;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceInteractibles;
import com.pacificbytestudios.projectciviccore.villagers.ai.actions.builders.general.BuilderEatTavernFoodAction;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;
import com.pacificbytestudios.projectciviccore.villagers.enums.VillagerMealType;

import io.sentry.util.Pair;

public class EatTavernFoodAction extends VillagerActionBase {
  private static final String FOOD_BLOCK_PREFIX = "Village_Food";
  private final VillagerMealType mealType;

  public EatTavernFoodAction(BuilderEatTavernFoodAction builderActionBase) {
    super(builderActionBase);
    this.mealType = builderActionBase.getMealType();
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

    WorkplaceInteractablesMapComponent interactables = this.getReservedTavernInteractibleComponent(ref, store);
    if (interactables == null) {
      return false;
    }

    UUIDComponent uuidComponent = require(store.getComponent(ref, UUIDComponent.getComponentType()), "UUID component");
    if (uuidComponent == null) {
      return false;
    }

    Interactable interactable = interactables.getInteractableOccupiedBy(
        WorkplaceInteractibles.EATING_SPOT,
        uuidComponent.getUuid());
    if (interactable == null) {
      return false;
    }

    Vector3i foodPosition = require(interactable.getDependsOn(), "tavern food position");
    if (foodPosition == null) {
      return false;
    }

    World world = store.getExternalData().getWorld();
    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(foodPosition.x, foodPosition.z));

    BlockType foodBlock = require(chunk.getBlockType(foodPosition), "food block");
    if (foodBlock == null || foodBlock.getId() == null || !foodBlock.getId().startsWith(FOOD_BLOCK_PREFIX)) {
      return false;
    }

    Pair<WorldChunk, Ref<ChunkStore>> blockEntityData = Utils.getBlockComponentEntityChunkAware(chunk, foodPosition);
    Ref<ChunkStore> foodRef = require(blockEntityData.getSecond(), "food block ref");
    if (!isRefValid(foodRef)) {
      return false;
    }

    VillageFoodComponent foodComponent = require(
        foodRef.getStore().getComponent(foodRef, VillageFoodComponent.getComponentType()),
        "village food component");
    if (foodComponent == null) {
      return false;
    }

    Utils.breakBlockChunkAware(blockEntityData.getFirst(), foodPosition);
    this.registerMealConsumption(ref, store);
    return true;
  }

  private void registerMealConsumption(
      Ref<EntityStore> ref,
      Store<EntityStore> store) {
    if (this.mealType == null) {
      return;
    }

    VillagerComponent villagerComponent = this.getVillagerComponent(ref, store);
    if (villagerComponent == null) {
      return;
    }

    WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
    if (worldTimeResource == null) {
      return;
    }

    villagerComponent.markMealAsConsumed(this.mealType, worldTimeResource.getGameTime());
    store.putComponent(ref, VillagerComponent.getComponentType(), villagerComponent);
  }

}
