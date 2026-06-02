package com.pacificbytestudios.projectciviccore.village.commands.coresubcommands;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D.Vec2i;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

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
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.enums.VillageHeartState;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;
import com.pacificbytestudios.projectciviccore.utils.Logger;

@Deprecated(forRemoval = false)
public class State extends CommandBase {
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

      Vec3i playerPos = Vec3i.from(transform.getPosition());
      EntityRef heartRef = villageStore.getVillageHeartIn(new Vec2i(playerPos.x, playerPos.z));

      if (heartRef == null) {
        context.sendMessage(Message.raw("No village found in range."));
        return;
      }

      Ref<ChunkStore> heart = heartRef.getRef();
      if (!heart.isValid()) {
        context.sendMessage(Message.raw("No valid village found in range."));
        return;
      }

      updateVillageState(context, state, heart);
    });
  }

  private boolean updateVillageState(CommandContext context, VillageHeartState state, Ref<ChunkStore> villageHeart) {
    boolean updated = false;

    VillageHeartComponent villageHeartComponent = villageHeart.getStore()
        .getComponent(villageHeart, VillageHeartComponent.getComponentType());

    if (villageHeartComponent == null) {
      Logger.logInfo("Could not fetch village heart component");
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
