package com.pacificbytestudios.projectciviccore.village.commands.villagersubcommands;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.villagersubcommands.subcommands.SitCommand;

public class Villager extends AbstractCommandCollection {

  public Villager() {
    super("npc", "Command npc");
    this.setPermissionGroup(GameMode.Creative);
    this.addSubCommand(new SitCommand());
  }

}
