package com.pacificbytestudios.projectciviccore.village.commands.simulationcommands.subcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.village.commands.core.VillageCommand;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionContext;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionResult;
import com.pacificbytestudios.projectciviccore.village.components.simulation.SimulationComponent;
import com.pacificbytestudios.projectciviccore.village.components.simulation.SimulationRunningComponent;

public class Pause extends VillageCommand {

  public Pause() {
    super("pause", "server.system.simulation.pause.desc");
  }

  @Override
  public void execute(ExecutionContext exeContext) {
    Ref<ChunkStore> heartRef = exeContext.village().getRef();
    SimulationComponent simulation = exeContext.chunkStore().getComponent(
        heartRef,
        SimulationComponent.getComponentType());

    if (simulation == null) {
      CommandUtils.sendExecutionResultMessage(
          exeContext.cmdContext(),
          ExecutionResult.Err,
          "server.system.simulation.pause.err.noSimulationFound");
      return;
    }

    SimulationRunningComponent running = exeContext.chunkStore().getComponent(
        heartRef,
        SimulationRunningComponent.getComponentType());
    if (running == null) {
      CommandUtils.sendExecutionResultMessage(
          exeContext.cmdContext(),
          ExecutionResult.Info,
          Message.translation("server.system.simulation.pause.err.alreadyPaused")
              .param("remainingDays", simulation.getRemainingDays()));
      return;
    }

    exeContext.chunkStore().removeComponentIfExists(
        heartRef,
        SimulationRunningComponent.getComponentType());

    CommandUtils.sendExecutionResultMessage(
        exeContext.cmdContext(),
        ExecutionResult.Ok,
        Message.translation("server.system.simulation.pause.success")
            .param("remainingDays", simulation.getRemainingDays()));
  }

}
