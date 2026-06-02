package com.pacificbytestudios.projectciviccore.village.commands.coresubcommands;

import java.util.Set;
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
import com.pacificbytestudios.projectciviccore.codecs.AreaFootprint;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.enums.VillageHeartState;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

@Deprecated(forRemoval = true)
public class Dump extends CommandBase {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  public Dump() {
    super("dump", "Print village data");
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
      if (!heart.isValid()) {
        context.sendMessage(Message.raw("No valid village found in range."));
        return;
      }

      VillageHeartComponent heartComponent = heart.getStore()
          .getComponent(heart,
              VillageHeartComponent.getComponentType());

      AreaFootprint area = heartComponent.getClaimedArea();
      Set<UUID> villagers = heartComponent.getVillagersRegistry();
      Set<UUID> unemployed = heartComponent.getUnemployedRegistry();
      int level = heartComponent.getLevel();
      VillageHeartState state = heartComponent.getState();

      context.sendMessage(Message.raw("=== Village Dump ==="));
      context.sendMessage(Message.raw("ClaimedArea: [" + area.getTopLeft() + " - " + area.getBotRight() + "]"));
      context.sendMessage(Message.raw("Level: " + level));
      context.sendMessage(Message.raw("State: " + state));

      printSet("Villagers", context, villagers);
      printSet("Unemployed", context, unemployed);

      System.out.println(heartComponent);
    });
  }

  private <T> void printSet(String category, CommandContext context, Set<T> setToPrint) {
    context.sendMessage(Message.raw(category + " count: " + setToPrint.size()));

    if (setToPrint.isEmpty()) {
      context.sendMessage(Message.raw(category + ": []"));
    } else {
      context.sendMessage(Message.raw(category + ":"));
      for (T element : setToPrint) {
        context.sendMessage(Message.raw(" - " + element));
      }
    }
  }

}
