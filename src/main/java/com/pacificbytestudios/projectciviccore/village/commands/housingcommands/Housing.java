package com.pacificbytestudios.projectciviccore.village.commands.housingcommands;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.housingcommands.subcommands.Despawn;
import com.pacificbytestudios.projectciviccore.village.commands.housingcommands.subcommands.Spawn;

public class Housing extends AbstractCommandCollection {

  public Housing() {
    super("house", "housing commands");
    this.setPermissionGroup(GameMode.Creative);
    this.addSubCommand(new Spawn());
    this.addSubCommand(new Despawn());
  }

}
