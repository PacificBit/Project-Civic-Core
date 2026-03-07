package com.pacificbytestudios.projectciviccore.village.commands.workplacesubcommands.subcommands;

import java.util.List;

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
import com.pacificbytestudios.projectciviccore.codecs.VillageHeartRef;
import com.pacificbytestudios.projectciviccore.utils.DespawnFunction;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.enums.WorkplaceType;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

public class Despawn extends CommandBase {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  @Nonnull
  private final RequiredArg<String> workplaceType = this.withRequiredArg(
      "type",
      "Type of workplace to despawn",
      ArgTypes.STRING);

  public Despawn() {
    super("despawn", "Despawn workplace");
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

      Vector3i playerPos = transform.getPosition().toVector3i();
      VillageHeartRef heartRef = villageStore.getVillageHeartIn(new Vector2i(playerPos.x, playerPos.z));

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

      WorkplaceType workplace = WorkplaceType.valueOf(this.workplaceType.get(context).toUpperCase());

      List<WorkplaceComponent> workplaceComponents = villageHeartComponent.getWorkplaceFrom(workplace);

      if (workplaceComponents.size() == 0) {
        return;
      }

      WorkplaceComponent toRemove = workplaceComponents.get(0);
      double shortestDistance = toRemove.getBenchPos().distanceSquaredTo(playerPos);

      for (int i = 1; i < workplaceComponents.size(); i++) {
        double distance = workplaceComponents.get(i).getBenchPos().distanceSquaredTo(playerPos);
        if (distance < shortestDistance) {
          toRemove = workplaceComponents.get(i);
          shortestDistance = distance;
        }
      }

      Vector3i benchPosition = toRemove.getBenchPos();

      WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(benchPosition.x, benchPosition.z));

      DespawnFunction despawnFunction = Utils.getDespawnFunction(workplace);
      boolean despawnResult = despawnFunction.apply(chunk, benchPosition);

      if (!despawnResult) {
        context.sendMessage(Message.raw("Could not despawn village"));
        return;
      }
      context.sendMessage(Message.raw("Village successfully despawned"));
    });
  }

}
