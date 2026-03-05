package com.pacificbytestudios.projectciviccore.village.commands;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.subcommands.Spawn;
import com.pacificbytestudios.projectciviccore.village.commands.subcommands.State;

public class VillageCommands extends AbstractCommandCollection {

  public VillageCommands() {
    super("village", "Village debug command tools");
    this.setPermissionGroup(GameMode.Creative);
    this.addSubCommand(new Spawn());
    this.addSubCommand(new State());
  }

}
