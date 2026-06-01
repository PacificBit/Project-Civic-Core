package com.pacificbytestudios.projectciviccore.village.commands.structurecommands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.structurecommands.subcommands.SpawnStructure;

public class Structure extends AbstractCommandCollection {

  public Structure() {
    super("structure", "server.system.structure.desc");
    this.addSubCommand(new SpawnStructure());
  }

}
