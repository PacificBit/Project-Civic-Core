
package com.pacificbytestudios.projectciviccore.villageheart.commands;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import com.pacificbytestudios.projectciviccore.villageheart.commands.subcommands.State;

public class VillageCommands extends AbstractCommandCollection {

  public VillageCommands() {
    super("village", "Village debug command tools");
    this.setPermissionGroup(GameMode.Creative);
    this.addSubCommand(new State());
  }

}
