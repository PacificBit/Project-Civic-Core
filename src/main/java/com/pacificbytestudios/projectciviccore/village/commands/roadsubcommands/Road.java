package com.pacificbytestudios.projectciviccore.village.commands.roadsubcommands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.roadsubcommands.subcommands.Despawn;
import com.pacificbytestudios.projectciviccore.village.commands.roadsubcommands.subcommands.Spawn;

public class Road extends AbstractCommandCollection {

  public Road() {
    super("road", "road commands");
    this.setPermissionGroups("hytale:WorldEditor");
    this.addSubCommand(new Spawn());
    this.addSubCommand(new Despawn());
  }

}
