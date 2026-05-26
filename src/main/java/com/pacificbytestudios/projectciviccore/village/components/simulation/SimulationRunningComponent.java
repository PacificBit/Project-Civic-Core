package com.pacificbytestudios.projectciviccore.village.components.simulation;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public class SimulationRunningComponent implements Component<ChunkStore> {
  public static final BuilderCodec<SimulationRunningComponent> CODEC = BuilderCodec
      .builder(SimulationRunningComponent.class, SimulationRunningComponent::new)
      .build();

  public SimulationRunningComponent clone() {
    return new SimulationRunningComponent();
  }

  public static ComponentType<ChunkStore, SimulationRunningComponent> getComponentType() {
    return ProjectCivicCore.get().getSimulationRunningComponentType();
  }
}
