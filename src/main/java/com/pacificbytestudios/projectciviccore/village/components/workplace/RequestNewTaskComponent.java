package com.pacificbytestudios.projectciviccore.village.components.workplace;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.UUID;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.ArrayDequeCodec;
import com.pacificbytestudios.projectciviccore.village.core.workplacetask.IWorkplaceTaskStrategy;

public class RequestNewTaskComponent implements Component<ChunkStore> {

  private static final CodecMapCodec<IWorkplaceTaskStrategy> STRATEGY_CODEC = new CodecMapCodec<>("Type");

  public static final BuilderCodec<RequestNewTaskComponent> CODEC = BuilderCodec
      .builder(RequestNewTaskComponent.class, RequestNewTaskComponent::new)

      .append(
          new KeyedCodec<>("Strategy", STRATEGY_CODEC),
          (obj, value) -> obj.strategy = value,
          obj -> obj.strategy)
      .add()

      .append(new KeyedCodec<>("WorkersUUIDs", new ArrayDequeCodec<>(Codec.UUID_BINARY, "WorkerUUID")),
          (obj, value) -> obj.workersId = (value == null ? new ArrayDeque<>() : value),
          (obj) -> obj.workersId)
      .add()

      .build();

  public static void registerStrategyCodec(
      String id,
      Class<? extends IWorkplaceTaskStrategy> strategyClass,
      Codec<? extends IWorkplaceTaskStrategy> codec) {
    STRATEGY_CODEC.register(id, strategyClass, codec);
  }

  public RequestNewTaskComponent() {
  }

  public RequestNewTaskComponent(IWorkplaceTaskStrategy strategy) {
    this.strategy = Objects.requireNonNull(strategy, "strategy");
  }

  private IWorkplaceTaskStrategy strategy;
  private ArrayDeque<UUID> workersId = new ArrayDeque<>();

  public IWorkplaceTaskStrategy getStrategy() {
    return this.strategy;
  }

  public void setStrategy(IWorkplaceTaskStrategy strategy) {
    this.strategy = Objects.requireNonNull(strategy, "strategy");
  }

  public UUID getNextWorkerId() {
    return this.workersId.pollFirst();
  }

  public void addWorkerId(UUID workerId) {
    this.workersId.add(workerId);
  }

  public boolean hasWorkerId() {
    return this.workersId.size() > 0;
  }

  @Override
  public RequestNewTaskComponent clone() {
    RequestNewTaskComponent component = new RequestNewTaskComponent();
    component.strategy = this.strategy.clone();
    component.workersId = this.workersId.clone();
    return component;
  }

  public static final ComponentType<ChunkStore, RequestNewTaskComponent> getComponentType() {
    return ProjectCivicCore.get().getRequestNewTaskComponentType();
  }

  @Override
  public String toString() {
    return "RequestNewTaskComponent{" +
        "strategy=" + strategy +
        ", workersId=" + workersId +
        '}';
  }

}
