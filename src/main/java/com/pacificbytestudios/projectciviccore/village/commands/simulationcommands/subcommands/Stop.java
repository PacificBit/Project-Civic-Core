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

public class Stop extends VillageCommand {

  public Stop() {
    super("stop", "server.system.simulation.stop.desc");
  }

  @Override
  public void execute(ExecutionContext exeContext) {
    Ref<ChunkStore> heartRef = exeContext.village().getRef();
    SimulationComponent simulation = exeContext.chunkStore().getComponent(
        heartRef,
        SimulationComponent.getComponentType());
    SimulationRunningComponent running = exeContext.chunkStore().getComponent(
        heartRef,
        SimulationRunningComponent.getComponentType());
    if (simulation == null && running == null) {
      CommandUtils.sendExecutionResultMessage(
          exeContext.cmdContext(),
          ExecutionResult.Info,
          "server.system.simulation.stop.err.noSimulationFound");
      return;
    }

    int remainingDays = simulation == null ? 0 : simulation.getRemainingDays();
    exeContext.chunkStore().removeComponentIfExists(
        heartRef,
        SimulationRunningComponent.getComponentType());
    exeContext.chunkStore().removeComponentIfExists(
        heartRef,
        SimulationComponent.getComponentType());

    CommandUtils.sendExecutionResultMessage(
        exeContext.cmdContext(),
        ExecutionResult.Ok,
        Message.translation("server.system.simulation.stop.success")
            .param("remainingDays", remainingDays));
  }

}
