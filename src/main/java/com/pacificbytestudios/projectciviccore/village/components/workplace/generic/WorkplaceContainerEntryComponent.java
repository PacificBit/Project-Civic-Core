package com.pacificbytestudios.projectciviccore.village.components.workplace.generic;

import java.util.UUID;

import org.joml.Vector3i;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.pacificbytestudios.projectciviccore.ProjectCivicCore;

public class WorkplaceContainerEntryComponent implements Component<ChunkStore> {
  public static final BuilderCodec<WorkplaceContainerEntryComponent> CODEC = BuilderCodec
      .builder(WorkplaceContainerEntryComponent.class,
          WorkplaceContainerEntryComponent::new)

      .append(new KeyedCodec<>("ContainerId", Codec.UUID_BINARY),
          (obj, value) -> obj.uuid = value,
          (obj) -> obj.uuid)
      .add()

      .append(new KeyedCodec<>("Position", Vector3iUtil.CODEC),
          (obj, value) -> obj.position = value,
          (obj) -> obj.position)
      .add()

      .append(new KeyedCodec<>("AccessPosition", Vector3iUtil.CODEC),
          (obj, value) -> obj.accessPosition = value,
          (obj) -> obj.accessPosition)
      .add()

      .build();

  private UUID uuid;
  private Vector3i position;
  private Vector3i accessPosition;

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public Vector3i getPosition() {
    return position;
  }

  public void setPosition(Vector3i position) {
    this.position = position;
  }

  public Vector3i getAccessPosition() {
    return this.accessPosition != null ? this.accessPosition : this.position;
  }

  public void setAccessPosition(Vector3i accessPosition) {
    this.accessPosition = accessPosition;
  }

  @Override
  public WorkplaceContainerEntryComponent clone() {
    WorkplaceContainerEntryComponent container = new WorkplaceContainerEntryComponent();
    container.uuid = this.uuid;
    container.position = this.position;
    container.accessPosition = this.accessPosition;
    return container;
  }

  public static ComponentType<ChunkStore, WorkplaceContainerEntryComponent> getComponentType() {
    return ProjectCivicCore.get().getWorkplaceContainerEntryComponentType();
  }

  @Override
  public String toString() {
    return "WorkplaceContainerEntryComponent{\n" +
        "uuid=" + uuid +
        "\n, position=" + position +
        "\n, accessPosition=" + accessPosition +
        '}';
  }
}
