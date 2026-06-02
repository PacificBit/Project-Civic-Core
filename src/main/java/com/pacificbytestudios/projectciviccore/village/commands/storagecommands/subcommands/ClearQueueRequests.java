package com.pacificbytestudios.projectciviccore.village.commands.storagecommands.subcommands;

import java.util.UUID;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D.Vec2i;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.storage.WorkplaceVillageStorageRequestComponent;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

@Deprecated(forRemoval = true)
public class ClearQueueRequests extends CommandBase {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  public ClearQueueRequests() {
    super("reset", "Reset the storage request queue");
  }

  @Override
  protected void executeSync(CommandContext context) {
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
      if (heart == null || !heart.isValid()) {
        context.sendMessage(Message.raw("No valid village found in range."));
        return;
      }

      VillageHeartComponent villageHeartComponent = heart.getStore()
          .getComponent(heart, VillageHeartComponent.getComponentType());
      if (villageHeartComponent == null) {
        context.sendMessage(Message.raw("Could not load village data."));
        return;
      }

      for (UUID storage : villageHeartComponent.getVillageStorageWorkplaces()) {
        Ref<ChunkStore> storageRef = villageHeartComponent.getWorkplaceFromId(storage).getWorkplaceRef().getRef();
        if (storageRef == null || !storageRef.isValid()) {
          context.sendMessage(Message.raw("Could not reset for storage: " + storage));
          continue;
        }

        WorkplaceVillageStorageRequestComponent storageComponent = storageRef.getStore().getComponent(storageRef,
            WorkplaceVillageStorageRequestComponent.getComponentType());

        storageComponent.clearAll();
      }
    });
  }

}
