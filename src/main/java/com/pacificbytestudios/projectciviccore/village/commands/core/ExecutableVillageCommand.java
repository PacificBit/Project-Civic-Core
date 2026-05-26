package com.pacificbytestudios.projectciviccore.village.commands.core;

import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionContext;

public interface ExecutableVillageCommand {

  public void execute(ExecutionContext exeContext);

}
