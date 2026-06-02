package com.pacificbytestudios.projectciviccore.village.commands.coresubcommands;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.pacificbytestudios.projectciviccore.codecs.EntityRef;
import com.pacificbytestudios.projectciviccore.utils.Logger;
import com.pacificbytestudios.projectciviccore.utils.StructurePlacementUtils;
import com.pacificbytestudios.projectciviccore.utils.VectorUtil;
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

    var playerTransform = exeContext.entityStore().getComponent(exeContext.playerRef(),
        TransformComponent.getComponentType());

    if (village != null) {
      sendVillageTooCloseMsg(exeContext.cmdContext(), village, playerTransform.getPosition());
      Logger.logInfo("Trying to spawn a village inside another village");
      return;
    }

    village = CommandUtils.doesVillagePlacementCollide(playerTransform);
    if (village != null) {
      sendVillageTooCloseMsg(exeContext.cmdContext(), village, playerTransform.getPosition());
      Logger.logInfo("Trying to spawn a village that would collide with another");
      return;
    }

    var playerPos = playerTransform.getPosition();
    WorldChunk chunk = exeContext.world().getChunk(ChunkUtil.indexChunkFromBlock(playerPos.x, playerPos.z));

    Rotation placementRotation = StructurePlacementUtils.getPlayerPlacementRotation(
        exeContext.entityStore(),
        exeContext.playerRef());

    var spawnPosition = resolveVillageAnchor(
        exeContext.world(),
        VectorUtil.toVector3i(playerPos));

    boolean spawned = TownHeartSpawner.spawn(chunk, spawnPosition, placementRotation);
    if (spawned) {
      CommandUtils.sendExecutionResultMessage(
          exeContext.cmdContext(),
          ExecutionResult.Ok,
          Message.translation("server.system.village.spawn.success")
              .param("position", VectorUtil.toString(spawnPosition)));
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
  private void sendVillageTooCloseMsg(CommandContext context, EntityRef village, Vector3d playerPos) {
    int maxVillageRadius = StructureConfig.TownHeart.DEFAULT_CONFIG.getMaxRadius();

    var villagePos = village.getPosition();
    var distance = Math.floor(villagePos.distance((int) playerPos.x, villagePos.y, (int) playerPos.z));

    CommandUtils.sendExecutionResultMessage(
        context,
        ExecutionResult.Err,
        Message.translation("server.system.village.spawn.err.tooClose")
            .param("villagePosition", VectorUtil.toString(villagePos))
            .param("minimumDistance", maxVillageRadius * 2)
            .param("currentDistance", distance));
  }

  /**
   * Returns the appropriate {@link Vector3i} where to spawn the village
   * 
   * @param world  World instance
   * @param origin Player position
   * @return Appropriate village spawn position
   */
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
