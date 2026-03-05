package com.pacificbytestudios.projectciviccore.village.commands.subcommands;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.enums.VillageHeartState;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry.ChunkLocation;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public class State extends CommandBase {
  private static final int COMMAND_CHUNK_RANGE = 2;
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  @Nonnull
  private final RequiredArg<String> stateType = this.withRequiredArg(
      "type",
      "Type of state to assign to the village",
      ArgTypes.STRING);

  public State() {
    super("state", "Set the state of the nearest village");
  }

  @Override
  protected void executeSync(CommandContext context) {
    VillageHeartState state = VillageHeartState.valueOf(stateType.get(context).toUpperCase());

    if (state == null) {
      context.sendMessage(Message.raw(
          "Invalid state. Use only one of: " + VillageHeartState.getAvailableStates()));
      return;
    }

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
      Ref<ChunkStore> heart = villageStore.getVillageHeartIn(new Vector2i(playerPos.x, playerPos.z));

      System.out.println("This is the heart: " + heart + " is valid? " + (heart == null ? "no" : heart.isValid()));
      updateVillageState(context, state, heart);
    });
  }

  private boolean updateVillageState(CommandContext context, VillageHeartState state, Ref<ChunkStore> villageHeart) {
    if (villageHeart == null) {
      context.sendMessage(Message.raw("No village found in range."));
      return false;
    }

    boolean updated = false;

    if (!villageHeart.isValid()) {
      ProjectCivicCore.LOGGER.atInfo().log("[updateVillageState] - Invalid village ref");
      villageStore.unregisterVillage(villageHeart);
      return false;
    }

    VillageHeartComponent villageHeartComponent = villageHeart.getStore()
        .getComponent(villageHeart, VillageHeartComponent.getComponentType());

    if (villageHeartComponent == null) {
      ProjectCivicCore.LOGGER.atInfo().log("[updateVillageState] - Could not fetch village heart component");
      return false;
    }

    String prevState = villageHeartComponent.getState().toString().toLowerCase();
    villageHeartComponent.setState(state);

    String message = "Changed state from: " + prevState + " to "
        + state.getState().toString().toLowerCase();
    context.sendMessage(Message.raw(message));

    updated = true;

    return updated;
  }
}
