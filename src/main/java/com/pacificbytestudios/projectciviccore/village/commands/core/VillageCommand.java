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
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;

public abstract class VillageCommand extends CommandBase implements ExecutableVillageCommand {

  private boolean isVillageRequire;

  public VillageCommand(String command, String description) {
    super(command, description);
    this.isVillageRequire = true;
  }

  public VillageCommand(String command, String description, boolean isVillageRequire) {
    super(command, description);
    this.isVillageRequire = isVillageRequire;
  }

  public VillageCommand(String command, String description, boolean isVillageRequire, boolean requireConfirmation) {
    super(command, description, requireConfirmation);
    this.isVillageRequire = isVillageRequire;
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

      if (this.isVillageRequire && (village == null || !village.getRef().isValid())) {
        CommandUtils.sendExecutionResultMessage(
            context,
            ExecutionResult.Err,
            "server.system.villagecommand.error.noValidVillage");
        ProjectCivicCore.LOGGER.atSevere()
            .log("[VillageCommand - executeSync] Could not fetch the village core block ref");
        return;
      }

      execute(new ExecutionContext(context, world, entityStore, chunkStore, playerRef, village));
    });
  }

  protected VillageHeartComponent getVillageHeart(ExecutionContext exeContext) {
    VillageHeartComponent heart = CommandUtils.getVillageHeartComponent(exeContext);

    if (heart == null) {
      CommandUtils.sendExecutionResultMessage(
          exeContext.cmdContext(),
          ExecutionResult.Err,
          "system.villagecommand.error.noVillageHeartBlock");

      ProjectCivicCore.LOGGER.atSevere()
          .log("[VillageCommand - getVillageHeart] Could not fetch the village core block");
      return null;
    }

    return heart;
  }
}
