package com.pacificbytestudios.projectciviccore.village.commands.subcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.utils.PrefabSpawner;
import com.pacificbytestudios.projectciviccore.village.core.VillageSpawner;

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

      Vector3i spawnPos = transform.getPosition().toVector3i();
      WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(spawnPos.x, spawnPos.z));

      for (int y = spawnPos.y; y > 0; y--) {
        if (chunk.getBlockType(spawnPos).getId().equals("Empty")) {
          spawnPos = new Vector3i(spawnPos.x, y, spawnPos.z);
        }
      }

      VillageSpawner.spawn(chunk, spawnPos);

      // boolean spawned = PrefabSpawner.spawnAssetPrefab(
      // world,
      // entityStore,
      // "Structures/WorkPlace/Town_Hall.prefab.json",
      // spawnPos,
      // Rotation.None,
      // true);
      //
      // if (!spawned) {
      // context.sendMessage(Message.raw("Prefab not found:
      // Structures/WorkPlace/Town_Hall.prefab.json"));
      // }
    });
  }

}
