package com.pacificbytestudios.projectciviccore.village.commands.core.utils;

import org.joml.Vector2i;
import org.joml.Vector3d;

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
import com.pacificbytestudios.projectciviccore.utils.Utils;
import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageBuilderComponent;
import com.pacificbytestudios.projectciviccore.village.components.builder.VillageTopologyComponent;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
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
    var playerPos = playerTransform.getPosition();
    return findVillageAtPlayerPosition(playerPos);
  }

  public static EntityRef findVillageAtPlayerPosition(Vector3d origin) {
    var xzCoords = new Vector2i((int) origin.x, (int) origin.z);
    return VillageHeartRegistry.get().getVillageHeartIn(xzCoords);
  }

  public static EntityRef doesVillagePlacementCollide(TransformComponent playerTransform) {
    var config = StructureConfig.TownHeart.DEFAULT_CONFIG;
    var radius = config.getMaxRadius();

    var playerPosition = VectorUtil.toVector3i(playerTransform.getPosition());
    AreaFootprint footprint = Utils.calculateAreaFootprint(playerPosition, radius);

    Vector2i topLeft = footprint.getTopLeft();
    Vector2i bottomRight = footprint.getBotRight();

    Vector2i[] corners = {
        topLeft,
        new Vector2i(topLeft.x, bottomRight.y),
        bottomRight,
        new Vector2i(bottomRight.x, topLeft.y)
    };

    VillageHeartRegistry registry = VillageHeartRegistry.get();

    for (Vector2i corner : corners) {
      EntityRef village = registry.getVillageHeartIn(corner);
      if (village != null) {
        return village;
      }
    }

    return null;
  }

  public static VillageHeartComponent getVillageHeartComponent(ExecutionContext context) {
    return context.chunkStore.getComponent(context.village().getRef(), VillageHeartComponent.getComponentType());
  }

  public static VillageTopologyComponent getVillageTopologyComponent(ExecutionContext context) {
    return context.chunkStore.getComponent(context.village().getRef(), VillageTopologyComponent.getComponentType());
  }

  public static VillageBuilderComponent getBuilderComponent(ExecutionContext context) {
    return context.chunkStore.getComponent(context.village().getRef(), VillageBuilderComponent.getComponentType());
  }

  public static void sendExecutionResultMessage(CommandContext context, ExecutionResult result, String message) {
    StringBuilder displayMsg = new StringBuilder();
    displayMsg.append("[").append(result.type).append("] ").append(message);
    context.sendMessage(Message.raw(displayMsg.toString()));
  }

  public static void sendExecutionResultMessage(CommandContext context, ExecutionResult result) {
    sendExecutionResultMessage(context, result, result.defaultMsg);
  }

  public enum ExecutionResult {
    Ok("Success", "Command executed successfully!"),
    Info("Info", "..."),
    Err("Error", "Could not execute command! see logs for more info");

    String type;
    String defaultMsg;

    private ExecutionResult(String type, String defaultMsg) {
      this.type = type;
      this.defaultMsg = defaultMsg;
    }

  }
}
