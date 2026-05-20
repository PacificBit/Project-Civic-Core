package com.pacificbytestudios.projectciviccore.village.commands.villagersubcommands.subcommands;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector2i;
import org.joml.Vector3i;

import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.TeleportVillagerToTownHeartTask;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

public class TeleportToHeartCommand extends CommandBase {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  @Nonnull
  private final RequiredArg<UUID> villagerId = this.withRequiredArg(
      "villagerId",
      "Villager UUID to teleport to the TownHeart",
      ArgTypes.UUID);

  public TeleportToHeartCommand() {
    super("tpheart", "Queue a villager teleport to the TownHeart");
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

      VillageHeartComponent villageHeart = heartRef.getStore()
          .getComponent(heartRef, VillageHeartComponent.getComponentType());
      if (villageHeart == null) {
        context.sendMessage(Message.raw("Could not load village data."));
        return;
      }

      UUID targetVillagerId = this.villagerId.get(context);
      if (villageHeart.getVillager(targetVillagerId) == null) {
        context.sendMessage(Message.raw("Villager is not registered in the current village."));
        return;
      }

      VillageTaskQueueComponent taskQueue = new VillageTaskQueueComponent();
      taskQueue.registerTask(new TeleportVillagerToTownHeartTask(targetVillagerId));
      Utils.safeVillageTaskEnqueue(heartRef, taskQueue);

      context.sendMessage(Message.raw("Queued TownHeart teleport for villager " + targetVillagerId));
    });
  }
}
