package com.pacificbytestudios.projectciviccore.village.commands.housingcommands.subcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import org.joml.Vector2i;
import org.joml.Vector3i;

import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.villagetask.VillageTaskQueueComponent;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.SpawnHouseTask;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

@Deprecated(forRemoval = true)
public class Spawn extends CommandBase {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  public Spawn() {
    super("spawn", "Spawn a house");
  }

  @Override
  protected void executeSync(CommandContext context) {
    Ref<EntityStore> playerRef = context.senderAsPlayerRef();
    Store<EntityStore> entityStore = playerRef.getStore();
    World world = entityStore.getExternalData().getWorld();

    world.execute(() -> {
      TransformComponent transform = entityStore.getComponent(playerRef, TransformComponent.getComponentType());

      Vector3i playerPos = VectorUtil.toVector3i(transform.getPosition());
      WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(playerPos.x, playerPos.z));

      for (int y = playerPos.y; y > 0; y--) {
        if (chunk.getBlockType(playerPos).getId().equals("Empty")) {
          playerPos = new Vector3i(playerPos.x, y + 2, playerPos.z);
        }
      }

      EntityRef heartRefWrapper = villageStore.getVillageHeartIn(new Vector2i(playerPos.x, playerPos.z));
      Ref<ChunkStore> heartRef = heartRefWrapper.getRef();
      if (heartRef == null || !heartRef.isValid()) {
        context.sendMessage(Message.raw("Could not get village reference"));
        return;
      }

      VillageTaskQueueComponent taskQueue = new VillageTaskQueueComponent();
      // taskQueue.registerTask(new SpawnHouseTask(playerPos));

      Utils.safeVillageTaskEnqueue(heartRef, taskQueue);

      context.sendMessage(Message.raw("Successfully register house spawn request"));
    });
  }

}
