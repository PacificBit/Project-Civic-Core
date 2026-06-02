package com.pacificbytestudios.projectciviccore.villagers.ai.actions.implementation.worker.innkeeper;

import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.codecs.Interactable;
import com.pacificbytestudios.projectciviccore.registry.components.VillageFoodComponent;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceInteractibles;
import com.pacificbytestudios.projectciviccore.villagers.core.utils.VillagerActionBase;

import io.sentry.util.Pair;

public class PlaceFoodOnPlateAction extends VillagerActionBase {

  public PlaceFoodOnPlateAction(BuilderActionBase builderActionBase) {
    super(builderActionBase);
  }

  @Override
  public boolean execute(
      Ref<EntityStore> ref,
      Role role,
      InfoProvider sensorInfo,
      double deltaTime,
      Store<EntityStore> store) {
    if (!canExecute(ref, role, sensorInfo, deltaTime, store)) {
      return false;
    }
    super.execute(ref, role, sensorInfo, deltaTime, store);

    WorkplaceInteractablesMapComponent interactables = this.getWorkplaceInteractibleComponent(ref, store);
    WorkplaceComponent workplaceComponent = this.getWorkplaceComponent(ref, store);
    if (interactables == null || workplaceComponent == null || workplaceComponent.getWorkplaceRef() == null) {
      return false;
    }

    UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());

    WorkplaceInteractibles group = WorkplaceInteractibles.EATING_SPOT;
    Interactable interactable = interactables.getInteractableOccupiedBy(group, uuidComponent.getUuid());
    if (interactable == null) {
      return false;
    }

    NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
    ItemStack item = npcEntity.getInventory().getActiveHotbarItem();
    if (item == null || item.getItemId() == null || !item.getItemId().startsWith("Village_Food")) {
      return false;
    }

    Vec3i platePosition = interactable.getDependsOn();
    World world = store.getExternalData().getWorld();
    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(platePosition.x, platePosition.z));

    boolean spawned = chunk.setBlock(
        platePosition.x, platePosition.y, platePosition.z,
        BlockType.getAssetMap().getAsset(item.getItemId()));

    if (!spawned) {
      return false;
    }

    Pair<WorldChunk, Ref<ChunkStore>> foodBlockEntity = Utils.getBlockComponentEntityChunkAware(chunk, platePosition);
    Ref<ChunkStore> foodRef = foodBlockEntity.getSecond();
    if (foodRef != null && foodRef.isValid()) {
      Store<ChunkStore> chunkStore = foodRef.getStore();
      VillageFoodComponent foodComponent = chunkStore.getComponent(foodRef, VillageFoodComponent.getComponentType());
      if (foodComponent == null) {
        foodComponent = new VillageFoodComponent();
      }
      Vec3i tavernPosition = workplaceComponent.getWorkplaceRef().getPosition();
      foodComponent.setTavernRef(
          world.getName() != null && tavernPosition != null
              ? new EntityRef(world.getName(), Vec3i.from(tavernPosition))
              : null);
      chunkStore.putComponent(foodRef, VillageFoodComponent.getComponentType(), foodComponent);
    }

    short activeSlot = npcEntity.getInventory().getActiveHotbarSlot();
    if (activeSlot < 0) {
      return false;
    }

    npcEntity.getInventory().getHotbar().setItemStackForSlot(
        activeSlot,
        item.withQuantity(item.getQuantity() - 1));

    interactable.setReady(true);
    interactable.release();
    Utils.markNeedSaving(world, workplaceComponent.getWorkplaceRef().getPosition());
    chunk.markNeedsSaving();
    return true;
  }

}
