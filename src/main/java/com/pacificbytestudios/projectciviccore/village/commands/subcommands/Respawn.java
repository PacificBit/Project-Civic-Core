package com.pacificbytestudios.projectciviccore.village.commands.subcommands;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.VillageHeartRef;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.core.spawner.VillageSpawner;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;
import com.pacificbytestudios.projectciviccore.villagers.components.VillagerComponent;

public class Respawn extends CommandBase {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  public Respawn() {
    super("respawn", "Start village from anew");
  }

  @Override
  protected void executeSync(CommandContext context) {
    Ref<EntityStore> playerRef = context.senderAsPlayerRef();
    Store<EntityStore> entityStore = playerRef.getStore();
    World world = entityStore.getExternalData().getWorld();

    world.execute(() -> {
      TransformComponent transform = entityStore.getComponent(
          playerRef,
          TransformComponent.getComponentType());

      if (transform == null) {
        context.sendMessage(Message.raw("Could not read player position."));
        return;
      }

      Vector3i playerPos = transform.getPosition().toVector3i();
      VillageHeartRef heartRef = villageStore.getVillageHeartIn(new Vector2i(playerPos.x, playerPos.z));

      if (heartRef == null) {
        context.sendMessage(Message.raw("No village found in range."));
        return;
      }

      Ref<ChunkStore> heart = heartRef.getRef();
      if (!heart.isValid()) {
        context.sendMessage(Message.raw("No valid village found in range."));
        return;
      }

      VillageHeartComponent villageHeartComponent = heart.getStore()
          .getComponent(heart, VillageHeartComponent.getComponentType());

      Vector3i heartPosition = heartRef.getPosition();
      world.getChunk(ChunkUtil.indexChunkFromBlock(heartPosition.x, heartPosition.z))
          .breakBlock(heartPosition.x,
              heartPosition.y, heartPosition.z);

      for (UUID villagerId : villageHeartComponent.getVillagersRegistry()) {
        VillagerComponent villager = villageHeartComponent.getVillager(villagerId);
        Vector3i bed = villager.getBedPosition();
        world.getChunk(ChunkUtil.indexChunkFromBlock(bed.x, bed.z)).breakBlock(bed.x, bed.y, bed.z);
        Ref<EntityStore> npc = world.getEntityStore().getRefFromUUID(villagerId);
        entityStore.removeEntity(npc, RemoveReason.REMOVE);
      }

      villageStore.unregisterVillage(villageHeartComponent.getUuid());

      WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(heartPosition.x, heartPosition.z));
      boolean spawnResult = VillageSpawner.spawn(chunk, heartPosition.clone().add(Vector3i.DOWN));

      if (!spawnResult) {
        context.sendMessage(Message.raw("Could not spawn village"));
        return;
      }
      context.sendMessage(Message.raw("Village successfully respawned"));
    });
  }

}
