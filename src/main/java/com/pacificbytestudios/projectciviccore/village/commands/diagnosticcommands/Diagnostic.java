package com.pacificbytestudios.projectciviccore.village.commands.diagnosticcommands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.diagnosticcommands.subcommands.Status;

public class Diagnostic extends AbstractCommandCollection {

  public Diagnostic() {
    super("diagnostic", "server.system.diagnostic.desc");
    this.addSubCommand(new Status());
  }

}
