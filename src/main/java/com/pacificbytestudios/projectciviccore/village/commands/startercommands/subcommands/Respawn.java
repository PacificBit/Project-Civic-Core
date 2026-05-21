package com.pacificbytestudios.projectciviccore.village.commands.startercommands.subcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import org.joml.Vector3i;
import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@Deprecated(forRemoval = true)
public class Respawn extends CommandBase {

  public Respawn() {
    super("respawn", "Respawn the current starter village");
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

      Vector3i villageAnchor = VectorUtil.clone(villageContext.heartWrapper().getPosition()).add(Vector3iUtil.DOWN);

      if (!StarterVillageSupport.despawnVillage(context, entityStore, world, villageContext)) {
        return;
      }

      if (!StarterVillageSupport.spawnStarterVillage(context, world, villageAnchor)) {
        return;
      }

      context.sendMessage(Message.raw(
          "Starter village successfully respawned with storage, farm, tavern, town_hall and 2 houses."));
    });
  }

}
