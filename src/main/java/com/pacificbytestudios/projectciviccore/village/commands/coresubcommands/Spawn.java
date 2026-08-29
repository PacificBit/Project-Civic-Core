package com.pacificbytestudios.projectciviccore.village.commands.coresubcommands;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3d;
import com.pacificbytestudios.projectciviccore.codecs.vectors.vector3D.Vec3i;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.VillageCommand;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionContext;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionResult;
import com.pacificbytestudios.projectciviccore.village.core.spawner.StructureConfig;
import com.pacificbytestudios.projectciviccore.village.core.spawner.TownHeartSpawner;

public class Spawn extends VillageCommand {

  public Spawn() {
    super("spawn", "server.system.village.spawn.desc", false);
  }

  /**
   * It tries to spawn a village. It will not spawn a village if either the player
   * is standing inside a village or if building a new village would make them
   * collide at any point
   *
   * @param exeContext Contains the command context and the player, current
   *                   position, village ref
   */
  @Override
  public void execute(ExecutionContext exeContext) {
    EntityRef village = exeContext.village();

    TransformComponent playerTransform = exeContext.entityStore().getComponent(exeContext.playerRef(),
        TransformComponent.getComponentType());

    Vec3d playerPos = Vec3d.from(playerTransform.getPosition());

    if (village != null) {
      sendVillageTooCloseMsg(exeContext.cmdContext(), village, playerPos);
      Logger.logInfo("Trying to spawn a village inside another village");
      return;
    }

    village = CommandUtils.doesVillagePlacementCollide(playerTransform);
    if (village != null) {
      sendVillageTooCloseMsg(exeContext.cmdContext(), village, playerPos);
      Logger.logInfo("Trying to spawn a village that would collide with another");
      return;
    }

    WorldChunk chunk = exeContext.world().getChunk(ChunkUtil.indexChunkFromBlock(playerPos.x, playerPos.z));

    Rotation placementRotation = StructurePlacementUtils.getPlayerPlacementRotation(
        exeContext.entityStore(),
        exeContext.playerRef());

    Vec3i spawnPosition = resolveVillageAnchor(
        exeContext.world(),
        Vec3i.from(playerPos));

    boolean spawned = TownHeartSpawner.spawn(chunk, spawnPosition, placementRotation);
    if (spawned) {
      CommandUtils.sendExecutionResultMessage(
          exeContext.cmdContext(),
          ExecutionResult.Ok,
          Message.translation("server.system.village.spawn.success")
              .param("position", spawnPosition.toString()));
      return;
    }

    CommandUtils.sendExecutionResultMessage(
        exeContext.cmdContext(),
        ExecutionResult.Err,
        "server.system.village.spawn.err.failed");
  }

  /**
   * Writes a message to the player console notifing the player that the new
   * village is too close
   * to an existing one
   * 
   * @param context Hytale's command context
   * @param village Reference of the already present village
   */
  private void sendVillageTooCloseMsg(CommandContext context, EntityRef village, Vec3d playerPos) {
    int maxVillageRadius = StructureConfig.TownHeart.DEFAULT_CONFIG.getMaxRadius();

    Vec3i villagePos = village.getPosition();
    double distance = Math.floor(villagePos.distance((int) playerPos.x, villagePos.y, (int) playerPos.z));

    CommandUtils.sendExecutionResultMessage(
        context,
        ExecutionResult.Err,
        Message.translation("server.system.village.spawn.err.tooClose")
            .param("villagePosition", villagePos.toString())
            .param("minimumDistance", maxVillageRadius * 2)
            .param("currentDistance", distance));
  }

  /**
   * Returns the appropriate {@link Vec3i} where to spawn the village
   * 
   * @param world  World instance
   * @param origin Player position
   * @return Appropriate village spawn position
   */
  private static Vec3i resolveVillageAnchor(World world, Vec3i origin) {
    WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(origin.x, origin.z));

    for (int y = origin.y; y > 0; y--) {
      Vec3i probePosition = new Vec3i(origin.x, y, origin.z);
      String blockId = world.getBlockType(probePosition.x, probePosition.y, probePosition.z).getId();
      if (blockId != null && !blockId.equals("Empty")) {
        return probePosition;
      }
    }

    return new Vec3i(origin.x, 0, origin.z);
  }

}
