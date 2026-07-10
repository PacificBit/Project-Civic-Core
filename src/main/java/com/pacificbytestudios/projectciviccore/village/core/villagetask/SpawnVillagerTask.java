package com.pacificbytestudios.projectciviccore.village.core.villagetask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.FastRandom;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Rotation3fc;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.Dirty;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.pacificbytestudio.codecbutler.annotations.SerialName;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3f;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.VillageBedComponent;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;
import com.pacificbytestudios.projectciviccore.villagers.enums.VillagerRole;

import it.unimi.dsi.fastutil.Pair;

@SerializableClass
public class SpawnVillagerTask extends AVillageTask {
  public static final BuilderCodec<SpawnVillagerTask> CODEC = SpawnVillagerTaskCodec.CODEC;

  private static final FastRandom random = new FastRandom();

  String npcRole;
  @SerialName("Position")
  int spawnRadius;

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
      Vec3i heartPos,
      VillageHeartComponent heart) {
    World world = chunk.getWorld();
    Store<EntityStore> entityStore = world.getEntityStore().getStore();
    Vec3d spawnPos = this.newSpawnPos(heartPos, this.spawnRadius);
    Rotation3fc rotation = Rotation3f.ZERO;
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

  private Vec3d newSpawnPos(Vec3i blockPos, int radius) {
    List<Vec3f> spawningOffsets = StructureConfig.TownHeart.getVillagerSpawningOffsets();

    if (!spawningOffsets.isEmpty()) {
      Vec3f selectedOffset = spawningOffsets.get(random.nextInt(spawningOffsets.size()));
      return Vec3d.from(blockPos).add(Vec3d.from(selectedOffset));
    }

    double angle = random.nextDouble() * Math.PI * 2.0;
    double distance = Math.sqrt(random.nextDouble()) * radius;
    double offsetX = Math.cos(angle) * distance;
    double offsetZ = Math.sin(angle) * distance;

    Vec3d spawnPos = new Vec3d(
        blockPos.x + offsetX,
        blockPos.y,
        blockPos.z + offsetZ);

    if (Vec3i.from(spawnPos).equals(blockPos)) {
      spawnPos.add(Vec3d.safeLeft());
    }

    return spawnPos;
  }

  private Ref<EntityStore> spawnNpc(
      Store<EntityStore> entityStore,
      Vec3d position,
      Rotation3fc rotation) {
    Pair<Ref<EntityStore>, INonPlayerCharacter> result = NPCPlugin.get()
        .spawnNPC(
            entityStore,
            this.npcRole,
            null,
            position,
            rotation);

    if (result == null) {
      return null;
    }

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
    Dirty dirty = entityStore.getComponent(npcRef, Dirty.getComponentType());

    if (storage == null || hotbar == null || utility == null || dirty == null) {
      Logger.logWarning(
          "Missing components for npc: " + npcRef);
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
      Logger.logWarning(
          "Villager inventory resize produced remainder items. npcRef=" +
              npcRef +
              ", remainder=" +
              remainder);
    }

    if (changed) {
      dirty.markDirty();
    }
  }
}
