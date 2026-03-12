package com.pacificbytestudios.projectciviccore.village.commands;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.coresubcommands.Despawn;
import com.pacificbytestudios.projectciviccore.village.commands.coresubcommands.Dump;
import com.pacificbytestudios.projectciviccore.village.commands.coresubcommands.Level;
import com.pacificbytestudios.projectciviccore.village.commands.coresubcommands.Respawn;
import com.pacificbytestudios.projectciviccore.village.commands.coresubcommands.Spawn;
import com.pacificbytestudios.projectciviccore.village.commands.coresubcommands.State;
import com.pacificbytestudios.projectciviccore.village.commands.storagecommands.Storage;
import com.pacificbytestudios.projectciviccore.village.commands.workplacesubcommands.Workplace;

public class VillageCommands extends AbstractCommandCollection {

  public VillageCommands() {
    super("village", "Village debug command tools");
    this.setPermissionGroup(GameMode.Creative);
    this.addSubCommand(new Workplace());
    this.addSubCommand(new Storage());
    this.addSubCommand(new Dump());
    this.addSubCommand(new Spawn());
    this.addSubCommand(new Despawn());
    this.addSubCommand(new Respawn());
    this.addSubCommand(new State());
    this.addSubCommand(new Level());
  }

}
