package com.pacificbytestudios.projectciviccore.village.commands.subcommands;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.VillageHeartRef;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

public class Level extends CommandBase {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  @Nonnull
  private final RequiredArg<Integer> level = this.withRequiredArg(
      "type",
      "Type of state to assign to the village",
      ArgTypes.INTEGER);

  public Level() {
    super("level", "Set the village level");
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

      VillageHeartComponent heartComponent = heart.getStore().getComponent(heart,
          VillageHeartComponent.getComponentType());

      Integer lvl = this.level.get(context);
      context.sendMessage(Message.raw("Setting village level from: " + heartComponent.getLevel() + " to " + lvl));
      heartComponent.setLevel(lvl);
    });
  }

}
