package com.pacificbytestudios.projectciviccore.village.commands.startercommands.subcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Despawn extends CommandBase {

  public Despawn() {
    super("despawn", "Despawn the current starter village");
  }

  @Override
  protected void executeSync(CommandContext context) {
    Ref<EntityStore> playerRef = context.senderAsPlayerRef();
    Store<EntityStore> entityStore = playerRef.getStore();
    World world = entityStore.getExternalData().getWorld();

    world.execute(() -> {
      StarterVillageSupport.VillageContext villageContext = StarterVillageSupport.findVillageFromPlayer(
          context,
          playerRef,
          entityStore);
      if (villageContext == null) {
        return;
      }

      if (!StarterVillageSupport.despawnVillage(context, entityStore, world, villageContext)) {
        return;
      }

      context.sendMessage(Message.raw("Starter village successfully despawned."));
    });
  }

}
