package com.pacificbytestudios.projectciviccore.village.commands.startercommands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

@Deprecated(forRemoval = true)
public class StarterVillage extends AbstractCommandCollection {

  public StarterVillage() {
    super("starter", "starter village commands");
    this.setPermissionGroups("hytale:WorldEditor");
    // this.addSubCommand(new Spawn());
    // this.addSubCommand(new Despawn());
    // this.addSubCommand(new Respawn());
  }

}
