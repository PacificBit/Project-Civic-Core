package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.FastRandom;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.VillageBedComponent;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.enums.VillagerRole;

import it.unimi.dsi.fastutil.Pair;

public class SpawnVillagerTask extends AVillageTask {
  public static final BuilderCodec<SpawnVillagerTask> CODEC = BuilderCodec
      .builder(SpawnVillagerTask.class, SpawnVillagerTask::new)

      .append(new KeyedCodec<>("NpcRole", Codec.STRING),
          (obj, value) -> obj.npcRole = value,
          (obj) -> obj.npcRole)
      .add()

      .append(new KeyedCodec<>("Position", Codec.INTEGER),
          (obj, value) -> obj.spawnRadius = value,
          obj -> obj.spawnRadius)
      .add()

      .build();

  private static final FastRandom random = new FastRandom();

  private String npcRole;
  private int spawnRadius;

  public SpawnVillagerTask() {
  }

  public SpawnVillagerTask(String npcRole, int spawnRadius) {
    this(npcRole, spawnRadius, null);
  }

  public SpawnVillagerTask(String npcRole, int spawnRadius, VillageBedComponent bed) {
    this.npcRole = npcRole;
    this.spawnRadius = spawnRadius;
  }

  @Override
  public void execute(
      WorldChunk chunk,
      Ref<ChunkStore> heartRef,
      Vector3i heartPos,
      VillageHeartComponent heart) {
    World world = chunk.getWorld();
    Store<EntityStore> entityStore = world.getEntityStore().getStore();
    Vector3d spawnPos = this.newSpawnPos(heartPos, this.spawnRadius);
    Vector3f rotation = new Vector3f(0f, 0f, 0f);
    Ref<EntityStore> npcRef = this.spawnNpc(entityStore, spawnPos, rotation);

    assert npcRef != null;

    VillagerComponent villagerComponent = new VillagerComponent();
    villagerComponent.setRole(VillagerRole.fromNpcRole(this.npcRole));

    UUIDComponent uuidComponent = entityStore.getComponent(npcRef, UUIDComponent.getComponentType());
    UUID uuid = uuidComponent.getUuid();
    heart.registerVillager(uuid, villagerComponent);
    heart.registerAsUnemployed(uuid);
    heart.registerAsHomeless(uuid);

    villagerComponent.setVillageHeartLocation(new EntityRef(world.getName(), heartPos));

    NPCEntity npcEntity = entityStore.getComponent(npcRef, NPCEntity.getComponentType());

    assert npcEntity != null;

    ensureVillagerInventoryCapacity(entityStore, npcRef);

    entityStore.putComponent(npcRef, Interactions.getComponentType(), buildInteractions());
    entityStore.ensureComponent(npcRef, Interactable.getComponentType());
    entityStore.putComponent(npcRef, VillagerComponent.getComponentType(), villagerComponent);
  }

  private Vector3d newSpawnPos(Vector3i blockPos, int radius) {
    List<Vector3i> spawningOffsets = StructureConfig.TownHeart.getVillagerSpawningOffsets();

    if (!spawningOffsets.isEmpty()) {
      Vector3i selectedOffset = spawningOffsets.get(random.nextInt(spawningOffsets.size()));
      Vector3i townHeartAnchor = blockPos.clone();

      return townHeartAnchor
          .add(selectedOffset)
          .toVector3d();
    }

    double angle = random.nextDouble() * Math.PI * 2.0;
    double distance = Math.sqrt(random.nextDouble()) * radius;
    double offsetX = Math.cos(angle) * distance;
    double offsetZ = Math.sin(angle) * distance;

    Vector3d spawnPos = new Vector3d(
        blockPos.getX() + offsetX,
        blockPos.getY(),
        blockPos.getZ() + offsetZ);

    if (spawnPos.toVector3i().equals(blockPos)) {
      spawnPos.add(Vector3i.LEFT);
    }

    return spawnPos;
  }

  private Ref<EntityStore> spawnNpc(
      Store<EntityStore> entityStore,
      Vector3d position,
      Vector3f rotation) {
    Pair<Ref<EntityStore>, INonPlayerCharacter> result = NPCPlugin.get()
        .spawnNPC(
            entityStore,
            this.npcRole,
            null,
            position,
            rotation);

    assert result != null;

    Ref<EntityStore> npcRef = result.key();
    return npcRef;
  }

  public static Interactions buildInteractions() {
    Map<InteractionType, String> interactions = new HashMap<>();
    interactions.put(InteractionType.Use, "Villager_Open_Inventory_Interaction");
    return new Interactions(interactions);
  }

  private static void ensureVillagerInventoryCapacity(
      Store<EntityStore> entityStore,
      Ref<EntityStore> npcRef) {
    InventoryComponent.Storage storage = entityStore.getComponent(npcRef,
        InventoryComponent.Storage.getComponentType());
    InventoryComponent.Hotbar hotbar = entityStore.getComponent(npcRef, InventoryComponent.Hotbar.getComponentType());
    InventoryComponent.Utility utility = entityStore.getComponent(npcRef,
        InventoryComponent.Utility.getComponentType());
    TransformComponent transform = entityStore.getComponent(npcRef, TransformComponent.getComponentType());

    if (storage == null || hotbar == null || utility == null || transform == null) {
      ProjectCivicCore.LOGGER.atSevere().log(
          "[SpawnVillagerTask] Missing inventory components for spawned villager. npcRef=" + npcRef);
      return;
    }

    List<ItemStack> remainder = new ArrayList<>();
    boolean changed = false;

    if (storage.getInventory().getCapacity() != VillagerComponent.STORAGE_CAPACITY) {
      storage.ensureCapacity(VillagerComponent.STORAGE_CAPACITY, remainder);
      changed = true;
    }

    if (hotbar.getInventory().getCapacity() != VillagerComponent.HOTBAR_CAPACITY) {
      hotbar.ensureCapacity(VillagerComponent.HOTBAR_CAPACITY, remainder);
      changed = true;
    }

    if (utility.getInventory().getCapacity() != VillagerComponent.OFFHAND_CAPACITY) {
      utility.ensureCapacity(VillagerComponent.OFFHAND_CAPACITY, remainder);
      changed = true;
    }

    if (!remainder.isEmpty()) {
      ProjectCivicCore.LOGGER.atWarning().log(
          "[SpawnVillagerTask] Villager inventory resize produced remainder items. npcRef=" +
              npcRef +
              ", remainder=" +
              remainder);
    }

    if (changed) {
      transform.markChunkDirty(entityStore);
    }
  }
}
