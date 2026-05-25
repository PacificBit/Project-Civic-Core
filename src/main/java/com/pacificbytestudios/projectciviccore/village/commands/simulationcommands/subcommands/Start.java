package com.pacificbytestudios.projectciviccore.village.commands.simulationcommands.subcommands;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.village.commands.core.VillageCommand;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionContext;
import com.pacificbytestudios.projectciviccore.village.commands.core.utils.CommandUtils.ExecutionResult;
import com.pacificbytestudios.projectciviccore.village.components.heart.VillageHeartComponent;
import com.pacificbytestudios.projectciviccore.village.components.simulation.SimulationComponent;
import com.pacificbytestudios.projectciviccore.village.components.simulation.SimulationRunningComponent;

public class Start extends VillageCommand {

  private static final String ADDED_KEY = "server.system.simulation.start.action";
  private static final String RESUME_KEY = "server.system.simulation.start.resume";
  private static final String START_KEY = "server.system.simulation.start.start";

  @Nonnull
  private final RequiredArg<Integer> days = this.withRequiredArg(
      "days", "server.system.simulation.start.param.day.description", ArgTypes.INTEGER);

  public Start() {
    super("start", "server.system.simulation.start.desc");
  }

  @Override
  public void execute(ExecutionContext exeContext) {
    VillageHeartComponent heart = this.getVillageHeart(exeContext);
    if (heart == null) {
      return;
    }

    int daysToSimulate = this.days.get(exeContext.cmdContext());
    if (daysToSimulate <= 0) {
      CommandUtils.sendExecutionResultMessage(
          exeContext.cmdContext(),
          ExecutionResult.Err,
          "server.system.simulation.start.err.invalidDays");
      return;
    }

    Ref<ChunkStore> heartRef = exeContext.village().getRef();
    boolean wasRunning = exeContext.chunkStore().getComponent(
        heartRef,
        SimulationRunningComponent.getComponentType()) != null;
    SimulationComponent simulation = exeContext.chunkStore().getComponent(
        heartRef,
        SimulationComponent.getComponentType());
    boolean hadSimulation = simulation != null;
    if (simulation == null) {
      simulation = new SimulationComponent();
    }

    simulation.addRemainingDays(daysToSimulate);
    exeContext.chunkStore().putComponent(
        heartRef,
        SimulationComponent.getComponentType(),
        simulation);
    exeContext.chunkStore().putComponent(
        heartRef,
        SimulationRunningComponent.getComponentType(),
        new SimulationRunningComponent());

    String translationKey = wasRunning ? ADDED_KEY : (hadSimulation ? RESUME_KEY : START_KEY);
    CommandUtils.sendExecutionResultMessage(
        exeContext.cmdContext(),
        ExecutionResult.Ok,
        Message.translation(translationKey)
            .param("addedDays", daysToSimulate)
            .param("remainingDays", simulation.getRemainingDays()));
  }

}
