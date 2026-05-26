package com.pacificbytestudios.projectciviccore.village.commands.diagnosticcommands.subcommands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;
import com.pacificbytestudios.projectciviccore.village.commands.core.VillageCommand;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionContext;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionResult;
import com.pacificbytestudios.projectciviccore.village.commands.diagnosticcommands.core.VillageRepairer;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;

public class Repair extends VillageCommand {

  public Repair() {
    super("repair", "server.system.diagnostic.repair.desc");
  }

  @Override
  public void execute(ExecutionContext exeContext) {
    VillageHeartComponent heart = this.getVillageHeart(exeContext);
    if (heart == null) {
      return;
    }

    World world = exeContext.world();
    boolean repaired = VillageRepairer.repairVillage(world, heart);
    CommandUtils.sendExecutionResultMessage(
        exeContext.cmdContext(),
        ExecutionResult.Ok,
        Message.raw("Repair status: " + repaired));
  }

}
