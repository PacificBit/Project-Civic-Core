package com.pacificbytestudios.projectciviccore.village.commands.housingcommands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.housingcommands.subcommands.Despawn;
import com.pacificbytestudios.projectciviccore.village.commands.housingcommands.subcommands.Spawn;

public class Housing extends AbstractCommandCollection {

  public Housing() {
    super("house", "housing commands");
    this.setPermissionGroups("hytale:WorldEditor");
    this.addSubCommand(new Spawn());
    this.addSubCommand(new Despawn());
  }

}
