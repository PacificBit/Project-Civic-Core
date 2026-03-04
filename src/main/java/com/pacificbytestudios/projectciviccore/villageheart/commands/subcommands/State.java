package com.pacificbytestudios.projectciviccore.villageheart.commands.subcommands;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
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
import com.pacificbytestudios.projectciviccore.villageheart.components.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.villageheart.state.VillageHeartState;
import com.pacificbytestudios.projectciviccore.villageheart.storage.VillageHeartStorage;
import com.pacificbytestudios.projectciviccore.villageheart.storage.VillageHeartStorage.ChunkLocation;

public class State extends CommandBase {
  private static VillageHeartStorage villageStore;

  @Nonnull
  private final RequiredArg<String> stateType = this.withRequiredArg(
      "type",
      "Type of state to assign to the village",
      ArgTypes.STRING);

  public State() {
    super("state", "Set the state of the nearest village");
    villageStore = VillageHeartStorage.get();
  }

  @Override
  protected void executeSync(CommandContext context) {
    VillageHeartState state = VillageHeartState.fromString(stateType.get(context));

    if (state == null) {
      String validStates = Stream.of(VillageHeartState.values())
          .map(VillageHeartState::getState)
          .collect(Collectors.joining(", "));

      context.sendMessage(Message.raw("Invalid state. Use only one of: " + validStates));
    }

    Ref<EntityStore> playerRef = context.senderAsPlayerRef();
    Store<EntityStore> entityStore = playerRef.getStore();
    World world = entityStore.getExternalData().getWorld();

    world.execute(() -> {
      TransformComponent component = entityStore.getComponent(playerRef, TransformComponent.getComponentType());

      assert component != null;

      Vector3i playerPos = component.getPosition().toVector3i();
      long chunkIndex = ChunkUtil.indexChunkFromBlock(playerPos.x, playerPos.z);
      WorldChunk chunk = world.getChunk(chunkIndex);

      assert chunk != null;

      ChunkLocation chunkLocation = new ChunkLocation(chunk.getX(), chunk.getZ());
      Set<Ref<ChunkStore>> villagesHeart = villageStore
          .getVillagesHeartIn(chunkLocation);

      if (villagesHeart.size() == 0) {
        String message = "No village found in the current chunk";
        context.sendMessage(Message.raw(message));
        return;
      }

      for (Ref<ChunkStore> heart : villagesHeart) {
        if (!heart.isValid()) {
          villageStore.unregisterVillage(chunkLocation, heart);
          continue;
        }

        VillageHeartComponent villageHeartComponent = heart.getStore()
            .getComponent(
                heart,
                VillageHeartComponent.getComponentType());

        String prevState = villageHeartComponent.getState().toString();
        villageHeartComponent.setState(state);

        String message = "Changed state from: " + prevState.toLowerCase() + " to: " + state.getState().toString();
        context.sendMessage(Message.raw(message));
      }
    });
  }

}
