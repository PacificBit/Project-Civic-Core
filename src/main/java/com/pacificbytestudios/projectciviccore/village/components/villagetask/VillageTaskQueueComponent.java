package com.pacificbytestudios.projectciviccore.village.components.villagetask;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.Predicate;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;
import com.pacificbytestudios.projectciviccore.codecs.ArrayDequeCodec;
import com.pacificbytestudios.projectciviccore.village.core.villagetask.AVillageTask;

public class VillageTaskQueueComponent implements Component<ChunkStore> {
  private static final CodecMapCodec<AVillageTask> TASK_CODEC = new CodecMapCodec<>("Type");
  private static final ArrayDequeCodec<AVillageTask> TASK_DEQUE_CODEC = new ArrayDequeCodec<>(
      TASK_CODEC,
      "VillageTaskDeque");

  public static final BuilderCodec<VillageTaskQueueComponent> CODEC = BuilderCodec
      .builder(VillageTaskQueueComponent.class, VillageTaskQueueComponent::new)

      .append(new KeyedCodec<>("Tasks", TASK_DEQUE_CODEC),
          (obj, value) -> obj.tasks = value == null ? new ArrayDeque<>() : value,
          obj -> obj.tasks)
      .add()

      .build();

  public static void registerTaskCodec(
      String id,
      Class<? extends AVillageTask> taskClass,
      Codec<? extends AVillageTask> codec) {
    TASK_CODEC.register(id, taskClass, codec);
  }

  private ArrayDeque<AVillageTask> tasks = new ArrayDeque<>();

  public void registerTask(AVillageTask task) {
    this.tasks.addLast(Objects.requireNonNull(task, "task"));
  }

  public AVillageTask pollTask() {
    return this.tasks.pollFirst();
  }

  public boolean hasAny() {
    return this.tasks.size() > 0;
  }

  public Iterable<AVillageTask> getTasks() {
    return this.tasks;
  }

  public int removeTasksIf(Predicate<AVillageTask> predicate) {
    if (predicate == null || this.tasks.isEmpty()) {
      return 0;
    }

    int removed = 0;
    ArrayDeque<AVillageTask> remainingTasks = new ArrayDeque<>();

    while (!this.tasks.isEmpty()) {
      AVillageTask task = this.tasks.pollFirst();

      if (task != null && predicate.test(task)) {
        removed += 1;
        continue;
      }

      remainingTasks.addLast(task);
    }

    this.tasks = remainingTasks;
    return removed;
  }

  public void merge(VillageTaskQueueComponent tasks) {
    while (tasks.hasAny()) {
      this.registerTask(tasks.pollTask());
    }
  }

  @Override
  public VillageTaskQueueComponent clone() {
    VillageTaskQueueComponent component = new VillageTaskQueueComponent();
    component.tasks = new ArrayDeque<>(this.tasks);
    return component;
  }

  public static ComponentType<ChunkStore, VillageTaskQueueComponent> getComponentType() {
    return ProjectCivicCore.get().getVillageTaskQueueComponentType();
  }
}
