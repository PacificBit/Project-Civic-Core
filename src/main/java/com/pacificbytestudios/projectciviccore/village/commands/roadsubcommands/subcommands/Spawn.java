package com.pacificbytestudios.projectciviccore.village.commands.roadsubcommands.subcommands;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import org.joml.Vector2i;
import org.joml.Vector3i;

import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.core.spawner.RoadSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig.Road.RoadLength;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig.Road.RoadType;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

public class Spawn extends CommandBase {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  @Nonnull
  private final RequiredArg<String> roadType = this.withRequiredArg(
      "type",
      "Road type to spawn",
      ArgTypes.STRING);

  @Nonnull
  private final RequiredArg<String> roadLength = this.withRequiredArg(
      "length",
      "Road length to spawn",
      ArgTypes.STRING);

  public Spawn() {
    super("spawn", "Spawn a road");
  }

  @Override
  protected void executeSync(CommandContext context) {
    Ref<EntityStore> playerRef = context.senderAsPlayerRef();
    Store<EntityStore> entityStore = playerRef.getStore();
    World world = entityStore.getExternalData().getWorld();

    world.execute(() -> {
      TransformComponent transform = entityStore.getComponent(playerRef, TransformComponent.getComponentType());
      if (transform == null) {
        context.sendMessage(Message.raw("Could not read player position."));
        return;
      }

      RoadType type;
      RoadLength length;
      try {
        type = parseRoadType(this.roadType.get(context));
        length = parseRoadLength(this.roadLength.get(context));
      } catch (IllegalArgumentException exception) {
        context.sendMessage(Message.raw(exception.getMessage()));
        return;
      }

      Vector3i playerPos = VectorUtil.toVector3i(transform.getPosition());
      Rotation placementRotation = StructurePlacementUtils.getPlayerPlacementRotation(entityStore, playerRef);
      WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(playerPos.x, playerPos.z));

      for (int y = playerPos.y; y > 0; y--) {
        if (chunk.getBlockType(playerPos).getId().equals("Empty")) {
          playerPos = new Vector3i(playerPos.x, y + 2, playerPos.z);
        }
      }

      EntityRef heartRefWrapper = villageStore.getVillageHeartIn(new Vector2i(playerPos.x, playerPos.z));
      if (heartRefWrapper == null) {
        context.sendMessage(Message.raw("No village found in range."));
        return;
      }

      Ref<ChunkStore> heartRef = heartRefWrapper.getRef();
      if (heartRef == null || !heartRef.isValid()) {
        context.sendMessage(Message.raw("No valid village found in range."));
        return;
      }

      VillageHeartComponent village = heartRef.getStore().getComponent(
          heartRef,
          VillageHeartComponent.getComponentType());
      if (village == null) {
        context.sendMessage(Message.raw("Could not load village data."));
        return;
      }

      boolean spawnResult = RoadSpawner.spawn(chunk, playerPos, placementRotation, village, type, length);
      if (!spawnResult) {
        context.sendMessage(Message.raw("Could not spawn road."));
        return;
      }

      context.sendMessage(Message.raw("Road successfully spawned."));
    });
  }

  private static RoadType parseRoadType(String value) {
    String normalized = normalize(value);
    return switch (normalized) {
      case "MAIN", "MAINROAD", "MAIN_ROAD" -> RoadType.MAIN_ROAD;
      case "SIDE", "SIDEROAD", "SIDE_ROAD" -> RoadType.SIDE_ROAD;
      default -> throw new IllegalArgumentException("Unknown road type. Use MAIN_ROAD or SIDE_ROAD.");
    };
  }

  private static RoadLength parseRoadLength(String value) {
    String normalized = normalize(value);
    return switch (normalized) {
      case "SMALL" -> RoadLength.SMALL;
      case "MEDIUM" -> RoadLength.MEDIUM;
      case "LARGE" -> RoadLength.LARGE;
      default -> throw new IllegalArgumentException("Unknown road length. Use SMALL, MEDIUM or LARGE.");
    };
  }

  private static String normalize(String value) {
    return value == null
        ? ""
        : value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
  }

}
