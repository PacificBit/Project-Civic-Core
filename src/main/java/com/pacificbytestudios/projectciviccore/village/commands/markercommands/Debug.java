package com.pacificbytestudios.projectciviccore.village.commands.markercommands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.markercommands.subcommands.ToggleWaypoints;

public class Debug extends AbstractCommandCollection {

  public Debug() {
    super("debug", "server.system.debug.desc");
    this.addSubCommand(new ToggleWaypoints());
  }

}
