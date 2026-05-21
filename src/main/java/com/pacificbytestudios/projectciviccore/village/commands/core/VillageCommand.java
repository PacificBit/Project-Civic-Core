package com.pacificbytestudios.projectciviccore.village.commands.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionContext;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionResult;

public abstract class VillageCommand extends CommandBase implements ExecutableVillageCommand {

  public VillageCommand(String command, String description) {
    super(command, description);
  }

  @Override
  protected void executeSync(CommandContext context) {
    Ref<EntityStore> playerRef = context.senderAsPlayerRef();

    if (playerRef == null || !playerRef.isValid()) {
      ProjectCivicCore.LOGGER.atSevere().log("[VillageCommand - executeSync] Could not get the player ref");
      return;
    }

    Store<EntityStore> entityStore = playerRef.getStore();
    World world = entityStore.getExternalData().getWorld();
    Store<ChunkStore> chunkStore = world.getChunkStore().getStore();

    world.execute(() -> {
      TransformComponent component = entityStore.getComponent(
          playerRef,
          TransformComponent.getComponentType());

      if (component == null) {
        CommandUtils.sendExecutionResultMessage(context, ExecutionResult.Err);
        ProjectCivicCore.LOGGER.atSevere()
            .log("[VillageCommand - executeSync] Could not fetch the player transform component");
        return;
      }

      EntityRef village = CommandUtils.findVillageAtPlayerPosition(component.getPosition());

      execute(new ExecutionContext(context, world, entityStore, chunkStore, playerRef, village));
    });
  }
}
