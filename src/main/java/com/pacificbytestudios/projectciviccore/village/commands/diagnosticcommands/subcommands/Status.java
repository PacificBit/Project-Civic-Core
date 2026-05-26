package com.pacificbytestudios.projectciviccore.village.commands.diagnosticcommands.subcommands;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.pacificbytestudios.projectciviccore.village.commands.core.VillageCommand;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionContext;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionResult;
import com.pacificbytestudios.projectciviccore.village.commands.diagnosticcommands.core.VillageRepairer;
import com.pacificbytestudios.projectciviccore.village.commands.diagnosticcommands.ui.VillageStatusPage;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.enums.StructureType;

public class Status extends VillageCommand {

  public Status() {
    super("status", "server.system.diagnostic.status.desc");
  }

  @Override
  public void execute(ExecutionContext exeContext) {
    VillageHeartComponent heart = this.getVillageHeart(exeContext);
    if (heart == null) {
      return;
    }

    World world = exeContext.world();

    boolean isMayorValid = VillageRepairer.isMayorValid(world, heart);

    Set<UUID> invalidNpcReference = VillageRepairer.getInvalidVillagerRef(world, heart);
    short level = (short) heart.getLevel();
    short villagerCount = heart.getVillagerCount();
    short homelessCount = heart.getHomelessCount();
    short emptyBedCount = heart.getAvailableBedsCount();
    short totalBedsCount = heart.getTotalBedsCount();
    short occupiedBeds = (short) (totalBedsCount - emptyBedCount);
    short unemployedCount = heart.getUnemployedCount();
    boolean isAnyWorkplaceEmpty = heart.getFirstWorkplaceWithMissingEmployee() != null;
    Map<StructureType, Short> structureTypeMap = heart.getStructureTypeCount();

    Player player = exeContext.entityStore().getComponent(exeContext.playerRef(), Player.getComponentType());
    PlayerRef hytalePlayerRef = exeContext.entityStore().getComponent(exeContext.playerRef(),
        PlayerRef.getComponentType());
    if (player == null || hytalePlayerRef == null) {
      CommandUtils.sendExecutionResultMessage(
          exeContext.cmdContext(),
          ExecutionResult.Err,
          Message.raw("Could not open the village status page."));
      return;
    }

    player.getPageManager().openCustomPage(
        exeContext.playerRef(),
        exeContext.entityStore(),
        new VillageStatusPage(
            hytalePlayerRef,
            world,
            heart,
            isMayorValid,
            invalidNpcReference,
            level,
            villagerCount,
            homelessCount,
            emptyBedCount,
            totalBedsCount,
            occupiedBeds,
            unemployedCount,
            isAnyWorkplaceEmpty,
            structureTypeMap));
  }

}
