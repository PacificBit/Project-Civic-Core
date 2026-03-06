package com.pacificbytestudios.projectciviccore.village.commands.workplacesubcommands;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.workplacesubcommands.subcommands.Despawn;
import com.pacificbytestudios.projectciviccore.village.commands.workplacesubcommands.subcommands.Spawn;

public class Workplace extends AbstractCommandCollection {

  public Workplace() {
    super("wp", "workplace commands");
    this.setPermissionGroup(GameMode.Creative);
    this.addSubCommand(new Spawn());
    this.addSubCommand(new Despawn());
  }

}
