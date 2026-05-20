package com.pacificbytestudios.projectciviccore.village.commands.workplacesubcommands.subcommands;

import java.util.UUID;

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
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnWorkplaceTask;
import com.pacificbytestudios.projectciviccore.village.enums.ConstructionMode;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

public class Spawn extends CommandBase {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  @Nonnull
  private final RequiredArg<String> workplaceType = this.withRequiredArg(
      "type",
      "Type of workplace to spawn",
      ArgTypes.STRING);

  public Spawn() {
    super("spawn", "Spawn a workplace");
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

      Vector3i playerPos = VectorUtil.toVector3i(transform.getPosition());
      Rotation placementRotation = StructurePlacementUtils.getPlayerPlacementRotation(entityStore, playerRef);
      WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(playerPos.x, playerPos.z));

      for (int y = playerPos.y; y > 0; y--) {
        if (chunk.getBlockType(playerPos).getId().equals("Empty")) {
          playerPos = new Vector3i(playerPos.x, y + 2, playerPos.z);
        }
      }

      EntityRef heartRefWrapper = villageStore.getVillageHeartIn(new Vector2i(playerPos.x, playerPos.z));

      String workplace = this.workplaceType.get(context).toUpperCase();
      Ref<ChunkStore> heartRef = heartRefWrapper.getRef();

      if (!heartRef.isValid()) {
        ProjectCivicCore.LOGGER.atSevere().log("[Workplace - Spawn] Could not find the village reference");
        context.sendMessage(Message.raw("Spawn of '" + workplace + "' failed, reason: could not locate village"));
        return;
      }

      VillageTaskQueueComponent taskQueue = new VillageTaskQueueComponent();
      StructureType workplaceType = StructureType.valueOf(workplace);
      SpawnWorkplaceTask spawnTask = new SpawnWorkplaceTask(
          UUID.randomUUID(),
          workplaceType,
          playerPos,
          placementRotation,
          1,
          ConstructionMode.COMPLETED);
      taskQueue.registerTask(spawnTask);

      Utils.safeVillageTaskEnqueue(heartRef, taskQueue);

      context.sendMessage(Message.raw(workplace + ": Successfully register spawn request"));
    });
  }

}
