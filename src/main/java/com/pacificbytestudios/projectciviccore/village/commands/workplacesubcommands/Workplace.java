package com.pacificbytestudios.projectciviccore.village.commands.workplacesubcommands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.workplacesubcommands.subcommands.Despawn;
import com.pacificbytestudios.projectciviccore.village.commands.workplacesubcommands.subcommands.Dump;
import com.pacificbytestudios.projectciviccore.village.commands.workplacesubcommands.subcommands.Spawn;

public class Workplace extends AbstractCommandCollection {

  public Workplace() {
    super("work", "workplace commands");
    this.setPermissionGroups("hytale:WorldEditor");
    this.addSubCommand(new Spawn());
    this.addSubCommand(new Despawn());
    this.addSubCommand(new Dump());
  }

}
