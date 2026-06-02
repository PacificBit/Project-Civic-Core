package com.pacificbytestudios.projectciviccore.village.commands.debugcommands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.pacificbytestudios.projectciviccore.village.commands.debugcommands.subcommands.Clear;
import com.pacificbytestudios.projectciviccore.village.commands.debugcommands.subcommands.Roads;
import com.pacificbytestudios.projectciviccore.village.commands.debugcommands.subcommands.Structures;

public class Debug extends AbstractCommandCollection {

    public Debug() {
        super("debug", "Debug");
        this.setPermissionGroups("hytale:WorldEditor");
        this.addSubCommand(new Roads());
        this.addSubCommand(new Structures());
        this.addSubCommand(new Clear());
    }
}
