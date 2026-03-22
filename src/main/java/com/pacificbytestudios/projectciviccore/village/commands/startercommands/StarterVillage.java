package com.pacificbytestudios.projectciviccore.village.commands.startercommands;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.startercommands.subcommands.Despawn;
import com.pacificbytestudios.projectciviccore.village.commands.startercommands.subcommands.Respawn;
import com.pacificbytestudios.projectciviccore.village.commands.startercommands.subcommands.Spawn;

public class StarterVillage extends AbstractCommandCollection {

  public StarterVillage() {
    super("starter", "starter village commands");
    this.setPermissionGroup(GameMode.Creative);
    this.addSubCommand(new Spawn());
    this.addSubCommand(new Despawn());
    this.addSubCommand(new Respawn());
  }

}
