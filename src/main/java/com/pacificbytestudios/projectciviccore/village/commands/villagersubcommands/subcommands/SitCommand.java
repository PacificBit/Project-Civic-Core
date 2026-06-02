package com.pacificbytestudios.projectciviccore.village.commands.villagersubcommands.subcommands;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.builtin.mounts.BlockMountAPI;
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
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;
import com.pacificbytestudios.projectciviccore.villagers.core.events.SitOnChairEvent;

@Deprecated(forRemoval = true)
public class SitCommand extends CommandBase {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  @Nonnull
  private final RequiredArg<org.joml.Vector3i> chairCoords = this.withRequiredArg(
      "coord",
      "Coord of the chair",
      ArgTypes.VECTOR3I);

  public SitCommand() {
    super("sit", "Make the npc sit at the given coords");
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

      if (villageHeartComponent.getUnemployedRegistry().isEmpty()) {
        context.sendMessage(Message.raw("No unemployed villager found."));
        return;
      }

      UUID villagerId = villageHeartComponent.getUnemployedRegistry().iterator().next();
      Ref<EntityStore> villagerRef = world.getEntityRef(villagerId);

      if (villagerRef == null || !villagerRef.isValid()) {
        context.sendMessage(Message.raw("Could not resolve villager ref."));
        return;
      }

      Vec3i chairPos = Vec3i.from(this.chairCoords.get(context));
      SitOnChairEvent event = new SitOnChairEvent(chairPos);
      entityStore.invoke(villagerRef, event);
      BlockMountAPI.BlockMountResult result = event.getResult();

      if (result == null) {
        context.sendMessage(Message.raw("Sit event was not handled."));
        return;
      }

      if (result instanceof BlockMountAPI.Mounted) {
        context.sendMessage(Message.raw("Villager seated."));
        return;
      }

      BlockMountAPI.DidNotMount failure = (BlockMountAPI.DidNotMount) result;
      context.sendMessage(Message.raw("Could not seat villager: " + failure.name()));
    });
  }

}
