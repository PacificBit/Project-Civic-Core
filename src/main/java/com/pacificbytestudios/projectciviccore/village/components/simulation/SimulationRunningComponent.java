package com.pacificbytestudios.projectciviccore.village.components.simulation;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

@SerializableClass
public class SimulationRunningComponent implements Component<ChunkStore> {
  public static final BuilderCodec<SimulationRunningComponent> CODEC = SimulationRunningComponentCodec.CODEC;

  public SimulationRunningComponent clone() {
    return SimulationRunningComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, SimulationRunningComponent> getComponentType() {
    return ProjectCivicCore.get().getSimulationRunningComponentType();
  }
}
