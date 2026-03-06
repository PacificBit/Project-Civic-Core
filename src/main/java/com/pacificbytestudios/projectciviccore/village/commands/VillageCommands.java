package com.pacificbytestudios.projectciviccore.village.commands;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.subcommands.Dump;
import com.pacificbytestudios.projectciviccore.village.commands.subcommands.Level;
import com.pacificbytestudios.projectciviccore.village.commands.subcommands.Respawn;
import com.pacificbytestudios.projectciviccore.village.commands.subcommands.Spawn;
import com.pacificbytestudios.projectciviccore.village.commands.subcommands.State;

public class VillageCommands extends AbstractCommandCollection {

  public VillageCommands() {
    super("village", "Village debug command tools");
    this.setPermissionGroup(GameMode.Creative);
    this.addSubCommand(new Dump());
    this.addSubCommand(new Spawn());
    this.addSubCommand(new Respawn());
    this.addSubCommand(new State());
    this.addSubCommand(new Level());
  }

}
