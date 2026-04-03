package com.pacificbytestudios.projectciviccore.village.commands.coresubcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.village.core.spawner.TownHeartSpawner;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

public class Spawn extends CommandBase {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

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
      if (transform == null) {
        context.sendMessage(Message.raw("Could not read player position."));
        return;
      }

      Vector3i playerPos = transform.getPosition().toVector3i();
      Rotation placementRotation = StructurePlacementUtils.getPlayerPlacementRotation(entityStore, playerRef);
      Vector3i villageAnchor = resolveVillageAnchor(world, playerPos);

      if (villageStore.getVillageHeartIn(new Vector2i(villageAnchor.x, villageAnchor.z)) != null) {
        context.sendMessage(Message.raw("A village already exists at this position."));
        return;
      }

      WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(villageAnchor.x, villageAnchor.z));
      boolean spawnResult = TownHeartSpawner.spawn(chunk, villageAnchor, placementRotation);

      if (!spawnResult) {
        context.sendMessage(Message.raw("Could not spawn village."));
        return;
      }
      context.sendMessage(Message.raw("Village successfully spawned"));
    });
  }

  private static Vector3i resolveVillageAnchor(World world, Vector3i origin) {
    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(origin.x, origin.z));

    for (int y = origin.y; y > 0; y--) {
      Vector3i probePosition = new Vector3i(origin.x, y, origin.z);
      String blockId = chunk.getBlockType(probePosition).getId();
      if (blockId != null && !blockId.equals("Empty")) {
        return probePosition;
      }
    }

    return new Vector3i(origin.x, 0, origin.z);
  }

}
