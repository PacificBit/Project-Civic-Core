package com.pacificbytestudios.projectciviccore.village.commands.housingcommands.subcommands;

import java.util.List;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import org.joml.Vector2i;
import org.joml.Vector3i;

import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.house.HouseComponent;
import com.pacificbytestudios.projectciviccore.village.core.spawner.HouseSpawner;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

public class Despawn extends CommandBase {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  public Despawn() {
    super("despawn", "Despawn house");
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

      Vector3i playerPos = VectorUtil.toVector3i(transform.getPosition());
      EntityRef heartRef = villageStore.getVillageHeartIn(new Vector2i(playerPos.x, playerPos.z));

      if (heartRef == null) {
        context.sendMessage(Message.raw("No village found in range."));
        return;
      }

      Ref<ChunkStore> heart = heartRef.getRef();
      if (heart == null || !heart.isValid()) {
        context.sendMessage(Message.raw("No valid village found in range."));
        return;
      }

      VillageHeartComponent villageHeartComponent = heart.getStore()
          .getComponent(heart, VillageHeartComponent.getComponentType());
      if (villageHeartComponent == null) {
        context.sendMessage(Message.raw("Could not load village data."));
        return;
      }

      List<HouseComponent> houses = villageHeartComponent.getHouseIdToHouse()
          .values()
          .stream()
          .toList();

      if (houses.size() == 0) {
        context.sendMessage(Message.raw("No houses found in this village."));
        return;
      }

      HouseComponent toRemove = houses.get(0);
      Vector3i coreBlockPosition = toRemove.getCoreBlockPosition();
      double shortestDistance = coreBlockPosition.distanceSquared(playerPos);

      for (int i = 1; i < houses.size(); i++) {
        HouseComponent candidate = houses.get(i);
        Vector3i candidatePosition = candidate.getCoreBlockPosition();
        double distance = candidatePosition.distanceSquared(playerPos);
        if (distance < shortestDistance) {
          toRemove = candidate;
          coreBlockPosition = candidatePosition;
          shortestDistance = distance;
        }
      }

      WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(coreBlockPosition.x, coreBlockPosition.z));

      boolean despawnResult = HouseSpawner.despawn(chunk, coreBlockPosition);

      if (!despawnResult) {
        context.sendMessage(Message.raw("Could not despawn house"));
        return;
      }
      context.sendMessage(Message.raw("House successfully despawned"));
    });
  }

}
