package com.pacificbytestudios.projectciviccore.village.commands.startercommands.subcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3i;

import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@Deprecated(forRemoval = true)
public class Spawn extends CommandBase {

  public Spawn() {
    super("spawn", "Spawn a starter village");
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

      Vector3i villageAnchor = StarterVillageSupport.resolveVillageAnchor(
          world,
          VectorUtil.toVector3i(transform.getPosition()));

      if (!StarterVillageSupport.spawnStarterVillage(context, world, villageAnchor)) {
        return;
      }

      context.sendMessage(Message.raw(
          "Starter village spawned and queued with storage, farm, tavern, town_hall and 2 houses."));
    });
  }

}
