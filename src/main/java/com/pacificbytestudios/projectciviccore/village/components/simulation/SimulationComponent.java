package com.pacificbytestudios.projectciviccore.village.components.simulation;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public class SimulationComponent implements Component<ChunkStore> {
  public static final BuilderCodec<SimulationComponent> CODEC = BuilderCodec
      .builder(SimulationComponent.class, SimulationComponent::new)

      .append(new KeyedCodec<>("RemainingDays", Codec.INTEGER),
          (obj, value) -> obj.remainingDays = value,
          obj -> obj.remainingDays)
      .add()

      .append(new KeyedCodec<>("ProcessedDays", Codec.INTEGER, false),
          (obj, value) -> obj.processedDays = value,
          obj -> obj.processedDays)
      .add()

      .build();

  private int remainingDays;
  private int processedDays;

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
    SimulationComponent component = new SimulationComponent();
    component.remainingDays = this.remainingDays;
    component.processedDays = this.processedDays;
    return component;
  }

  public static ComponentType<ChunkStore, SimulationComponent> getComponentType() {
    return ProjectCivicCore.get().getSimulationComponentType();
  }
}
