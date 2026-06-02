package com.pacificbytestudios.projectciviccore.village.commands.markercommands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.markercommands.subcommands.Draw;

public class Markers extends AbstractCommandCollection {

  public Markers() {
    super("markers", "server.system.markers.desc");
    this.addSubCommand(new Draw());
  }

}
