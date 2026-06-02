package com.pacificbytestudios.projectciviccore.village.commands.debugcommands.subcommands;

import com.pacificbytestudios.projectciviccore.utils.DrawUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.VillageCommand;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionContext;

public class Clear extends VillageCommand {

    public Clear() {
        super("clear", "Clear all debug shapes", false);
    }

    @Override
    public void execute(ExecutionContext exeContext) {
        DrawUtils.clear(exeContext.world());
    }
}
