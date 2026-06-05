package com.pacificbytestudios.projectciviccore.village.commands.core.utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.pacificbytestudios.projectciviccore.codecs.AreaFootprint;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector2D.Vec2i;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageBuilderComponent;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.workplace.townhall.TownHallComponent;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;
import com.pacificbytestudios.projectciviccore.village.registry.VillageHeartRegistry;

public class CommandUtils {

  public record ExecutionContext(
      CommandContext cmdContext,
      World world,
      Store<EntityStore> entityStore,
      Store<ChunkStore> chunkStore,
      Ref<EntityStore> playerRef,
      EntityRef village) {
  }

  public static EntityRef findVillageAtPlayerPosition(TransformComponent playerTransform) {
    Vec3d playerPos = Vec3d.from(playerTransform.getPosition());
    return findVillageAtPlayerPosition(playerPos);
  }

  public static EntityRef findVillageAtPlayerPosition(Vec3d origin) {
    var xzCoords = new Vec2i((int) origin.x, (int) origin.z);
    return VillageHeartRegistry.get().getVillageHeartIn(xzCoords);
  }

  public static EntityRef doesVillagePlacementCollide(TransformComponent playerTransform) {
    var config = StructureConfig.TownHeart.DEFAULT_CONFIG;
    var radius = config.getMaxRadius();

    Vec3i playerPosition = Vec3i.from(playerTransform.getPosition());
    AreaFootprint footprint = Utils.calculateAreaFootprint(playerPosition, radius);

    Vec2i topLeft = footprint.getTopLeft();
    Vec2i bottomRight = footprint.getBotRight();

    Vec2i[] corners = {
        topLeft,
        new Vec2i(topLeft.x, bottomRight.y),
        bottomRight,
        new Vec2i(bottomRight.x, topLeft.y)
    };

    VillageHeartRegistry registry = VillageHeartRegistry.get();

    for (Vec2i corner : corners) {
      EntityRef village = registry.getVillageHeartIn(corner);
      if (village != null) {
        return village;
      }
    }

    return null;
  }

  public static VillageHeartComponent getVillageHeartComponent(ExecutionContext context) {
    return context.chunkStore().getComponent(context.village().getRef(), VillageHeartComponent.getComponentType());
  }

  public static TownHallComponent getTownHallComponent(ExecutionContext context, VillageHeartComponent heart) {
    return context.chunkStore().getComponent(heart.getTownHallRef(), TownHallComponent.getComponentType());
  }

  public static void sendExecutionResultMessage(CommandContext context, ExecutionResult result) {
    sendExecutionResultMessage(context, result, result.defaultMsg);
  }

  public static void sendExecutionResultMessage(CommandContext context, ExecutionResult result, String translationKey) {
    sendExecutionResultMessage(context, result, Message.translation(translationKey));
  }

  public static void sendExecutionResultMessage(CommandContext context, ExecutionResult result, Message message) {
    String msgPrefix = "[%s] ".formatted(result.type);
    context.sendMessage(Message.join(Message.raw(msgPrefix), message));
  }

  public enum ExecutionResult {
    Ok("Success", Message.translation("server.system.commandutils.defaultMsg.ok")),
    Info("Info", Message.raw("...")),
    Err("Error", Message.translation("server.system.commandutils.defaultMsg.err"));

    String type;
    Message defaultMsg;

    private ExecutionResult(String type, Message defaultMsg) {
      this.type = type;
      this.defaultMsg = defaultMsg;
    }

  }
}
