package com.pacificbytestudios.projectciviccore.village.commands.workplacesubcommands.subcommands;

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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.village.core.spawner.BakerySpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.FarmSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.MineSpawner;
import com.pacificbytestudios.projectciviccore.village.core.spawner.SawmillSpawner;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceType;

public class Spawn extends CommandBase {

  @Nonnull
  private final RequiredArg<String> workplaceType = this.withRequiredArg(
      "type",
      "Type of workplace to spawn",
      ArgTypes.STRING);

  public Spawn() {
    super("spawn", "Spawn a workplace");
  }

  @Override
  protected void executeSync(CommandContext context) {
    Ref<EntityStore> playerRef = context.senderAsPlayerRef();
    Store<EntityStore> entityStore = playerRef.getStore();
    World world = entityStore.getExternalData().getWorld();

    world.execute(() -> {
      TransformComponent transform = entityStore.getComponent(playerRef, TransformComponent.getComponentType());

      Vector3i villageCorePosition = transform.getPosition().toVector3i();
      WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(villageCorePosition.x, villageCorePosition.z));

      for (int y = villageCorePosition.y; y > 0; y--) {
        if (chunk.getBlockType(villageCorePosition).getId().equals("Empty")) {
          villageCorePosition = new Vector3i(villageCorePosition.x, y + 2, villageCorePosition.z);
        }
      }

      boolean spawnResult = false;
      String workplace = this.workplaceType.get(context).toUpperCase();

      switch (WorkplaceType.valueOf(workplace)) {
        case BAKERY:
          spawnResult = BakerySpawner.spawn(chunk, villageCorePosition);
          break;
        case MINE:
          spawnResult = MineSpawner.spawn(chunk, villageCorePosition);
          break;
        case SAWMILL:
          spawnResult = SawmillSpawner.spawn(chunk, villageCorePosition);
          break;
        case FARM:
          spawnResult = FarmSpawner.spawn(chunk, villageCorePosition);
          break;
        default:
          break;
      }

      if (!spawnResult) {
        context.sendMessage(Message.raw("Could not spawn " + workplace));
        return;
      }
      context.sendMessage(Message.raw(workplace + " successfully spawned"));
    });
  }

}
