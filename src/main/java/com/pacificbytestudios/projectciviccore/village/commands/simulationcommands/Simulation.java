package com.pacificbytestudios.projectciviccore.village.commands.simulationcommands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.simulationcommands.subcommands.Start;
import com.pacificbytestudios.projectciviccore.village.commands.simulationcommands.subcommands.Pause;
import com.pacificbytestudios.projectciviccore.village.commands.simulationcommands.subcommands.Resume;
import com.pacificbytestudios.projectciviccore.village.commands.simulationcommands.subcommands.Stop;

public class Simulation extends AbstractCommandCollection {

  public Simulation() {
    super("sim", "server.system.simulation.desc");
    this.setPermissionGroups("hytale:WorldEditor");
    this.addSubCommand(new Start());
    this.addSubCommand(new Pause());
    this.addSubCommand(new Resume());
    this.addSubCommand(new Stop());
  }

}
