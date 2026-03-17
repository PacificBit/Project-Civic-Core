package com.pacificbytestudios.projectciviccore.village.commands.coresubcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.village.core.spawner.TownHeartSpawner;

public class Spawn extends CommandBase {

  public Spawn() {
    super("spawn", "Spawn a village under player");
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

      boolean spawnResult = TownHeartSpawner.spawn(chunk, villageCorePosition);

      if (!spawnResult) {
        context.sendMessage(Message.raw("Could not spawn village"));
        return;
      }
      context.sendMessage(Message.raw("Village successfully spawned"));
    });
  }

}
