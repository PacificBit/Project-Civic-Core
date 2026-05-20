package com.pacificbytestudios.projectciviccore.village.commands.workplacesubcommands.subcommands;

import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector2i;
import org.joml.Vector3i;

import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
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
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.generic.WorkplaceInteractablesMapComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.tavern.TavernComponent;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

public class Dump extends CommandBase {
  private static final VillageHeartRegistry villageStore = VillageHeartRegistry.get();

  @Nonnull
  private final RequiredArg<String> workplaceType = this.withRequiredArg(
      "type",
      "Type of workplace to despawn",
      ArgTypes.STRING);

  public Dump() {
    super("dump", "Dump workplace data");
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

      Vector3i playerPos = VectorUtil.toVector3i(transform.getPosition());
      EntityRef heartRef = villageStore.getVillageHeartIn(new Vector2i(playerPos.x, playerPos.z));

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

      StructureType workplace = StructureType.valueOf(this.workplaceType.get(context).toUpperCase());

      List<WorkplaceComponent> workplaceComponents = villageHeartComponent.getWorkplaceFrom(workplace);

      if (workplaceComponents.size() == 0) {
        return;
      }

      WorkplaceComponent closestWorkplace = workplaceComponents.get(0);
      double shortestDistance = closestWorkplace.getBenchPos().distanceSquared(playerPos);

      for (int i = 1; i < workplaceComponents.size(); i++) {
        double distance = workplaceComponents.get(i).getBenchPos().distanceSquared(playerPos);
        if (distance < shortestDistance) {
          closestWorkplace = workplaceComponents.get(i);
          shortestDistance = distance;
        }
      }

      System.out
          .println("==================Dump: " + this.workplaceType.get(context).toUpperCase() + "====================");
      System.out.println(closestWorkplace);

      if (closestWorkplace.getWorkplaceType().equals(StructureType.TAVERN)) {
        System.out
            .println("==================Dump: TAVERN COMPONENT====================");
        TavernComponent tavern = heart.getStore().getComponent(closestWorkplace.getWorkplaceRef().getRef(),
            TavernComponent.getComponentType());
        System.out.println(tavern);
      }

      System.out
          .println("==================Dump: INTERACTABLES====================");

      WorkplaceInteractablesMapComponent interactables = heart.getStore().getComponent(
          closestWorkplace.getWorkplaceRef().getRef(),
          WorkplaceInteractablesMapComponent.getComponentType());
      System.out.println(interactables);
    });
  }

}
