package com.pacificbytestudios.projectciviccore.village.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.coresubcommands.Despawn;
import com.pacificbytestudios.projectciviccore.village.commands.coresubcommands.Spawn;
import com.pacificbytestudios.projectciviccore.village.commands.diagnosticcommands.Diagnostic;
import com.pacificbytestudios.projectciviccore.village.commands.markercommands.Debug;
import com.pacificbytestudios.projectciviccore.village.commands.simulationcommands.Simulation;
import com.pacificbytestudios.projectciviccore.village.commands.structurecommands.Structure;

public class VillageCommands extends AbstractCommandCollection {

  public VillageCommands() {
    super("village", "Village debug command tools");
    this.setPermissionGroups("hytale:WorldEditor");

    // Core sub commands
    this.addSubCommand(new Spawn());
    this.addSubCommand(new Despawn());
    this.addSubCommand(new Simulation());

    // Diagnostic sub commands
    this.addSubCommand(new Diagnostic());

    // Structure sub commands
    this.addSubCommand(new Structure());

    // Markers sub commands
    this.addSubCommand(new Debug());
  }

}
