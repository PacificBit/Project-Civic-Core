package com.pacificbytestudios.projectciviccore.village.components.simulation;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudio.codecbutler.annotations.SerializableClass;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

@SerializableClass
public class SimulationComponent implements Component<ChunkStore> {
  public static final BuilderCodec<SimulationComponent> CODEC = SimulationComponentCodec.CODEC;

  int remainingDays;
  int processedDays;

  public int getRemainingDays() {
    return remainingDays;
  }

  public void setRemainingDays(int remainingDays) {
    this.remainingDays = Math.max(0, remainingDays);
  }

  public void addRemainingDays(int days) {
    this.remainingDays += days;
  }

  public int getProcessedDays() {
    return processedDays;
  }

  public void incrementProcessedDays() {
    this.processedDays += 1;
  }

  @Override
  public SimulationComponent clone() {
    return SimulationComponentCodec.clone(this);
  }

  public static ComponentType<ChunkStore, SimulationComponent> getComponentType() {
    return ProjectCivicCore.get().getSimulationComponentType();
  }
}
